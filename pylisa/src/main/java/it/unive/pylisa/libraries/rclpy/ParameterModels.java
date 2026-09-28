package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.type.PyExceptionType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What the parameter methods of an rclpy node do to the program state.
 * <p>
 * rclpy keeps the declared parameters of a node in a dictionary from their
 * name to a {@code Parameter} object. Here the dictionary is a chain: the
 * field {@value #PARAMETERS} of the node refers to the most recently declared
 * {@code Parameter}, whose field {@value #NEXT} refers to the one declared
 * before it, down to {@code None}. Looking up a name walks the chain and
 * compares names: where a comparison is undecided, both "found here" and "look
 * further" are explored, and neither is refined by the comparison, so the
 * outcome holds also when an entry stands for several parameters (declared in
 * a loop, say).
 * </p>
 * <p>
 * When parameters change in ways the chain does not track (several at once,
 * undeclared, set by other nodes while the node is spun), the node's field
 * {@value #CHANGED} becomes true, and from then on its parameters are
 * unknown.
 * </p>
 * <p>
 * Values given from outside the program come from the
 * {@link ParameterOverrides} the client of the analysis supplies.
 * </p>
 */
final class ParameterModels {

	/**
	 * The field of a node that refers to the most recently declared parameter.
	 */
	static final String PARAMETERS = "$parameters";

	/**
	 * The field of a node that tells whether its parameters were changed in
	 * ways the chain does not track.
	 */
	static final String CHANGED = "$parameters_changed";

	/**
	 * The field of a node that tells whether undeclared parameters may be
	 * read.
	 */
	static final String ALLOW_UNDECLARED = "$allow_undeclared_parameters";

	/**
	 * The field of a node that tells whether the program gave it no
	 * parameter values of its own, neither as {@code parameter_overrides} nor
	 * in {@code cli_args}.
	 */
	static final String NO_PROGRAM_OVERRIDES = "$no_parameter_overrides";

	/**
	 * The field of a node that tells whether it uses the arguments of the
	 * process and of {@code rclpy.init}.
	 */
	static final String USE_GLOBAL_ARGUMENTS = "$use_global_arguments";

	/**
	 * The field of a node that tells whether a callback that may reject
	 * parameter values was registered.
	 */
	static final String SET_CALLBACKS = "$set_parameters_callbacks";

	/**
	 * The field of a parameter that refers to the parameter declared before
	 * it on the same node.
	 */
	static final String NEXT = "$next";

	/**
	 * The field of a parameter that tells whether it is read-only.
	 */
	static final String READ_ONLY = "$read_only";

	/**
	 * The field of a parameter that tells whether it has no value (type
	 * {@code NOT_SET}).
	 */
	static final String NOT_SET = "$not_set";

	/**
	 * The field of a parameter that tells whether its type may change.
	 */
	static final String DYNAMIC = "$dynamic_typing";

	private static final String NAME = "name";

	private static final String VALUE = "value";

	private static final String TYPE = "type_";

	/**
	 * The parameter every node declares when it is created, through its time
	 * source.
	 */
	private static final String USE_SIM_TIME = "use_sim_time";

	private ParameterModels() {
	}

	/**
	 * The value of a declared parameter, and whether it has none.
	 *
	 * @param value  the value
	 * @param notSet whether the parameter has no value
	 */
	private record Declared(SymbolicExpression value, SymbolicExpression notSet) {
	}

	/**
	 * Initializes the parameters of a new node, as its constructor does: none
	 * is declared by the program yet, and the time source of the node declares
	 * {@code use_sim_time}.
	 *
	 * @param <A>                  the kind of abstract state
	 * @param <D>                  the kind of abstract domain
	 * @param state                the state
	 * @param build                the factory of expressions
	 * @param site                 the location of the call creating the node
	 * @param node                 a reference to the node
	 * @param programOverrides     the {@code parameter_overrides} argument
	 * @param cliArgs              the {@code cli_args} argument
	 * @param useGlobalArguments   the {@code use_global_arguments} argument
	 * @param allowUndeclared      the {@code allow_undeclared_parameters}
	 *                                 argument
	 * @param declareFromOverrides the
	 *                                 {@code automatically_declare_parameters_from_overrides}
	 *                                 argument
	 *
	 * @return the state after the initialization
	 *
	 * @throws SemanticException if the initialization cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> initialize(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression programOverrides,
			SymbolicExpression cliArgs,
			SymbolicExpression useGlobalArguments,
			SymbolicExpression allowUndeclared,
			SymbolicExpression declareFromOverrides)
			throws SemanticException {
		// parameters declared automatically from overrides have names the
		// analysis does not know
		ModelState<A, D> initialized = state.write(node, PARAMETERS, build.none())
				.write(node, CHANGED, declareFromOverrides)
				.write(node, ALLOW_UNDECLARED, allowUndeclared)
				.write(node, USE_GLOBAL_ARGUMENTS, useGlobalArguments)
				.write(node, SET_CALLBACKS, build.bool(false));
		ModelState<A, D> placed = initialized.ifNone(programOverrides,
				(noOverrides, o) -> noOverrides.ifNone(cliArgs,
						(nothing, a) -> nothing.write(node, NO_PROGRAM_OVERRIDES, build.bool(true)),
						(arguments, a) -> arguments.write(node, NO_PROGRAM_OVERRIDES, build.bool(false))),
				(overrides, o) -> overrides.write(node, NO_PROGRAM_OVERRIDES, build.bool(false)));
		// the time source declares use_sim_time unless it is already declared
		SymbolicExpression name = build.string(USE_SIM_TIME);
		CodeLocation timeSite = new TaggedLocation(site, USE_SIM_TIME);
		ModelState.Step<A, D, SymbolicExpression> declareIt = (missing, none) -> declare(missing, build, timeSite,
				node, name, build.bool(false), build.none(), build.bool(false));
		return withChanged(placed, node,
				(unknown, c) -> unknown.lub(declareIt.apply(unknown, build.none())),
				(known, c) -> lookup(known, build, node, name, (declared, entry) -> declared, declareIt));
	}

	/**
	 * Declares a parameter, as {@code Node.declare_parameter(name, value,
	 * descriptor, ignore_override)} does, and returns it.
	 *
	 * @param <A>            the kind of abstract state
	 * @param <D>            the kind of abstract domain
	 * @param state          the state
	 * @param build          the factory of expressions
	 * @param site           the allocation site of the parameter
	 * @param node           a reference to the node
	 * @param name           the name of the parameter
	 * @param defaultValue   the value given by the program
	 * @param descriptor     the descriptor given by the program, or
	 *                           {@code None}
	 * @param ignoreOverride whether values given from outside are ignored
	 *
	 * @return the state after the declaration
	 *
	 * @throws SemanticException if the declaration cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> declare(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression name,
			SymbolicExpression defaultValue,
			SymbolicExpression descriptor,
			SymbolicExpression ignoreOverride)
			throws SemanticException {
		ModelState<A, D> result = state.unreachable();
		// a name that is not a string, and a default that is not a parameter
		// value (a dictionary, a list of mixed types, Parameter.Type.NOT_SET)
		if (mayBe(state, name, t -> !t.isStringType()))
			result = result.lub(state.raise(PyExceptionType.TYPE_ERROR));
		if (mayBe(state, defaultValue, t -> !isPlain(t)))
			result = result.lub(state.raise(PyExceptionType.TYPE_ERROR))
					.lub(state.raise(PyExceptionType.VALUE_ERROR));
		ModelState.Step<A, D, SymbolicExpression> declareNew = (fresh, none) -> dynamicTyping(fresh, build,
				defaultValue, descriptor,
				(typed, dynamic) -> value(typed, build, node, name, defaultValue, dynamic, ignoreOverride,
						(valued, declared) -> checkedBySetCallbacks(valued, node,
								(accepted, n) -> append(accepted, build, site, node, name, declared, dynamic,
										descriptor))));
		return result.lub(state.branch(build.equal(name, build.string("")),
				(empty, c) -> empty.raise(RclpyExceptions.INVALID_PARAMETER),
				(named, c) -> withChanged(named, node,
						(unknown, c1) -> unknown.raise(RclpyExceptions.PARAMETER_ALREADY_DECLARED)
								.lub(declareNew.apply(unknown, build.none())),
						(known, c1) -> lookup(known, build, node, name,
								(duplicate, entry) -> duplicate
										.raise(RclpyExceptions.PARAMETER_ALREADY_DECLARED),
								declareNew))));
	}

	/**
	 * Reads a parameter, as {@code Node.get_parameter(name)} does.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param site  the allocation site of the parameter returned for an
	 *                  undeclared name, when undeclared parameters are allowed
	 * @param node  a reference to the node
	 * @param name  the name of the parameter
	 *
	 * @return the state after the read, whose computed value is the parameter
	 *
	 * @throws SemanticException if the read cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> get(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression name)
			throws SemanticException {
		return withChanged(state, node,
				(unknown, c) -> unknown.returning(build.unknown())
						.lub(unknown.raise(RclpyExceptions.PARAMETER_NOT_DECLARED))
						.lub(unknown.raise(RclpyExceptions.PARAMETER_UNINITIALIZED)),
				(known, c) -> lookup(known, build, node, name,
						// a statically typed parameter without value cannot
						// be read
						(found, entry) -> withField(found, entry, NOT_SET,
								(checked, notSet) -> checked.branch(notSet,
										(unset, c1) -> withField(unset, entry, DYNAMIC,
												(typed, dynamic) -> typed.branch(dynamic,
														(anyType, c2) -> anyType.returning(entry),
														(fixedType, c2) -> fixedType.raise(
																RclpyExceptions.PARAMETER_UNINITIALIZED))),
										(set, c1) -> set.returning(entry))),
						(missing, none) -> withField(missing, node, ALLOW_UNDECLARED,
								(checked, allowed) -> checked.branch(allowed,
										(allowing, c1) -> notSet(allowing, build, site, name),
										(forbidding, c1) -> forbidding
												.raise(RclpyExceptions.PARAMETER_NOT_DECLARED)))));
	}

	/**
	 * Reads a parameter or yields an alternative, as
	 * {@code Node.get_parameter_or(name, alternative_value)} does: the
	 * alternative is returned when the parameter is not declared or has no
	 * value.
	 *
	 * @param <A>         the kind of abstract state
	 * @param <D>         the kind of abstract domain
	 * @param state       the state
	 * @param build       the factory of expressions
	 * @param site        the allocation site of the parameter returned when
	 *                        the alternative is {@code None}
	 * @param node        a reference to the node
	 * @param name        the name of the parameter
	 * @param alternative the alternative, or {@code None}
	 *
	 * @return the state after the read, whose computed value is the parameter
	 *
	 * @throws SemanticException if the read cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> getOr(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression name,
			SymbolicExpression alternative)
			throws SemanticException {
		ModelState.Step<A, D, SymbolicExpression> otherwise = (absent, none) -> absent.ifNone(alternative,
				(byDefault, c) -> notSet(byDefault, build, site, name),
				(given, c) -> given.returning(alternative));
		return withChanged(state, node,
				(unknown, c) -> unknown.returning(build.unknown()).lub(otherwise.apply(unknown, build.none())),
				(known, c) -> lookup(known, build, node, name,
						(found, entry) -> withField(found, entry, NOT_SET,
								(checked, notSet) -> checked.branch(notSet,
										(unset, c1) -> otherwise.apply(unset, build.none()),
										(set, c1) -> set.returning(entry))),
						otherwise));
	}

	/**
	 * Tells whether a parameter is declared, as
	 * {@code Node.has_parameter(name)} does.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param node  a reference to the node
	 * @param name  the name of the parameter
	 *
	 * @return the state after the check, whose computed value is the answer
	 *
	 * @throws SemanticException if the check cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> has(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name)
			throws SemanticException {
		return withChanged(state, node,
				(unknown, c) -> unknown.returning(build.unknown(BoolType.INSTANCE)),
				(known, c) -> lookup(known, build, node, name,
						(found, entry) -> found.returning(build.bool(true)),
						(missing, none) -> missing.returning(build.bool(false))));
	}

	/**
	 * Undeclares a parameter, as {@code Node.undeclare_parameter(name)}
	 * does. The chain is not shortened: the parameters of the node become
	 * unknown instead.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param node  a reference to the node
	 * @param name  the name of the parameter
	 *
	 * @return the state after the undeclaration
	 *
	 * @throws SemanticException if the undeclaration cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> undeclare(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name)
			throws SemanticException {
		return withChanged(state, node,
				(unknown, c) -> markChanged(unknown, build, node).returning(build.none())
						.lub(unknown.raise(RclpyExceptions.PARAMETER_NOT_DECLARED))
						.lub(unknown.raise(RclpyExceptions.PARAMETER_IMMUTABLE)),
				(known, c) -> lookup(known, build, node, name,
						(found, entry) -> withField(found, entry, READ_ONLY,
								(checked, readOnly) -> checked.branch(readOnly,
										(immutable, c1) -> immutable.raise(RclpyExceptions.PARAMETER_IMMUTABLE),
										(mutable, c1) -> markChanged(mutable, build, node)
												.returning(build.none()))),
						(missing, none) -> missing.raise(RclpyExceptions.PARAMETER_NOT_DECLARED)));
	}

	/**
	 * Records that the parameters of a node were changed in ways the chain
	 * does not track, as declaring or setting several parameters at once does,
	 * and yields an unknown result; the call may raise any of the exceptions
	 * of these methods.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param node  a reference to the node
	 *
	 * @return the state after the change
	 *
	 * @throws SemanticException if the change cannot be recorded
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> changeUntracked(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node)
			throws SemanticException {
		ModelState<A, D> changed = markChanged(state, build, node);
		return raiseAny(changed.returning(build.unknown()), changed,
				PyExceptionType.TYPE_ERROR,
				PyExceptionType.VALUE_ERROR,
				RclpyExceptions.INVALID_PARAMETER,
				RclpyExceptions.INVALID_PARAMETER_VALUE,
				RclpyExceptions.PARAMETER_NOT_DECLARED,
				RclpyExceptions.PARAMETER_ALREADY_DECLARED,
				RclpyExceptions.INVALID_PARAMETER_TYPE,
				RclpyExceptions.PARAMETER_IMMUTABLE,
				RclpyExceptions.PARAMETER_UNINITIALIZED);
	}

	/**
	 * Reads parameters in ways whose results are not tracked (several at once,
	 * their types, their descriptors): the result is unknown, and the call may
	 * raise for undeclared or uninitialized parameters.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 *
	 * @return the state after the read
	 *
	 * @throws SemanticException if the read cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> readUntracked(
			ModelState<A, D> state,
			Expressions build)
			throws SemanticException {
		return raiseAny(state.returning(build.unknown()), state,
				RclpyExceptions.PARAMETER_NOT_DECLARED,
				RclpyExceptions.PARAMETER_UNINITIALIZED);
	}

	/**
	 * Registers a callback that is called when parameters are declared or
	 * set, and may reject them. The callback is not run.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param node  a reference to the node
	 *
	 * @return the state after the registration
	 *
	 * @throws SemanticException if the registration cannot be recorded
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> addSetCallback(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node)
			throws SemanticException {
		return state.write(node, SET_CALLBACKS, build.bool(true)).returning(build.none());
	}

	/**
	 * Records that other nodes may change the parameters of a node from now
	 * on, if the client of the analysis says they may: the node is being spun
	 * or was added to an executor, so its parameter services answer requests.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param node  a reference to the node
	 *
	 * @return the state after the change
	 *
	 * @throws SemanticException if the change cannot be recorded
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> exposed(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node)
			throws SemanticException {
		if (!ParameterOverrides.current().remoteChangesPossible())
			return state;
		return markChanged(state, build, node);
	}

	/**
	 * Initializes a {@code Parameter} object, as
	 * {@code Parameter.__init__(self, name, type_, value)} does.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param self  a reference to the parameter
	 * @param name  the name
	 * @param type  the type, or {@code None} to infer it from the value
	 * @param value the value
	 *
	 * @return the state after the initialization
	 *
	 * @throws SemanticException if the fields cannot be written
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> initializeParameter(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression self,
			SymbolicExpression name,
			SymbolicExpression type,
			SymbolicExpression value)
			throws SemanticException {
		ModelState<A, D> result = state.unreachable();
		// a value of no parameter type, and a value that does not agree with
		// the given type
		if (mayBe(state, value, t -> !isPlain(t)))
			result = result.lub(state.raise(PyExceptionType.TYPE_ERROR));
		ModelState<A, D> named = state.write(self, NAME, name)
				.write(self, VALUE, value)
				.write(self, NOT_SET, notSetOf(state, build, value))
				.write(self, DYNAMIC, build.bool(false));
		return result.lub(named.ifNone(type,
				(inferred, c) -> inferred.write(self, TYPE, build.unknown()).returning(build.none()),
				(given, c) -> given.write(self, TYPE, type).returning(build.none())
						.lub(given.raise(PyExceptionType.VALUE_ERROR))));
	}

	/**
	 * Looks a name up in the chain of parameters of a node.
	 *
	 * @param found   what to do where the name is declared, with a reference
	 *                    to its parameter
	 * @param missing what to do where it is not
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> lookup(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name,
			ModelState.Step<A, D, SymbolicExpression> found,
			ModelState.Step<A, D, SymbolicExpression> missing)
			throws SemanticException {
		return withField(state, node, PARAMETERS,
				(start, head) -> walk(start, build, name, head, Set.of(), found, missing));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> walk(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression name,
			SymbolicExpression link,
			Set<String> visited,
			ModelState.Step<A, D, SymbolicExpression> found,
			ModelState.Step<A, D, SymbolicExpression> missing)
			throws SemanticException {
		ModelState<A, D> result = state.unreachable();
		if (!certainly(state, link, Type::isPointerType))
			// the end of the chain: the name is not declared
			result = result.lub(missing.apply(state, build.none()));
		Set<String> entries = state.objects(link);
		// an entry already visited has had all its successors explored,
		// the end of the chain included
		if (entries.isEmpty() || visited.containsAll(entries))
			return result;
		Set<String> seen = new HashSet<>(visited);
		seen.addAll(entries);
		return result.lub(withField(state, link, NAME,
				(current, entryName) -> current.branch(build.equal(entryName, name),
						(hit, c) -> found.apply(hit, link),
						(miss, c) -> withField(miss, link, NEXT,
								(further, next) -> walk(further, build, name, next, seen, found, missing)))));
	}

	/**
	 * Computes whether the type of a parameter may change: a parameter
	 * declared with neither a value nor a descriptor has a dynamic type, one
	 * declared with a value and no descriptor a static one, and otherwise the
	 * descriptor tells. A descriptor may also reject the value with its
	 * ranges, or conflict with a type given instead of a value.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> dynamicTyping(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression defaultValue,
			SymbolicExpression descriptor,
			ModelState.Step<A, D, SymbolicExpression> typed)
			throws SemanticException {
		return state.ifNone(descriptor,
				(plain, d) -> plain.ifNone(defaultValue,
						(nameOnly, v) -> typed.apply(nameOnly, build.bool(true)),
						(withValue, v) -> typed.apply(withValue, build.bool(false))),
				(described, d) -> withField(described, descriptor, "dynamic_typing", typed)
						.lub(described.raise(RclpyExceptions.INVALID_PARAMETER_VALUE))
						.lub(described.raise(PyExceptionType.VALUE_ERROR)));
	}

	/**
	 * Computes the value a declared parameter gets. In order: the values the
	 * program gives the node itself (its {@code parameter_overrides} and
	 * {@code cli_args}), then, if the node uses global arguments, those given
	 * to {@code rclpy.init} and those of the command line of the process (see
	 * {@link ParameterOverrides}); otherwise the default.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> value(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name,
			SymbolicExpression defaultValue,
			SymbolicExpression dynamic,
			SymbolicExpression ignoreOverride,
			ModelState.Step<A, D, Declared> valued)
			throws SemanticException {
		Declared plain = new Declared(storedDefault(state, build, defaultValue),
				notSetOf(state, build, defaultValue));
		ModelState.Step<A, D, SymbolicExpression> overridden = (current, x) -> overridden(current, build,
				defaultValue, dynamic, valued);
		ParameterOverrides overrides = ParameterOverrides.current();
		ModelState.Step<A, D, SymbolicExpression> hook = (current, x) -> fromHook(current, build, node, name,
				defaultValue, dynamic, plain, overrides, overrides.known(), valued);
		ModelState.Step<A, D, SymbolicExpression> global = (current, x) -> withField(current, node,
				USE_GLOBAL_ARGUMENTS,
				(checked, useGlobal) -> checked.branch(useGlobal,
						(using, c) -> withField(using, node, NodeModel.CONTEXT,
								(inContext, context) -> withField(inContext, context, ContextModel.ARGS_GIVEN,
										(checkedArgs, given) -> checkedArgs.branch(given,
												(fromProgram, c1) -> overridden.apply(fromProgram, x)
														.lub(hook.apply(fromProgram, x)),
												(fromProcess, c1) -> hook.apply(fromProcess, x)))),
						(local, c) -> valued.apply(local, plain)));
		return state.branch(ignoreOverride,
				(ignored, c) -> valued.apply(ignored, plain),
				(considered, c) -> withField(considered, node, NO_PROGRAM_OVERRIDES,
						(checked, none) -> checked.branch(none,
								(onlyOutside, c1) -> global.apply(onlyOutside, none),
								(given, c1) -> overridden.apply(given, none).lub(global.apply(given, none)))));
	}

	/**
	 * The value given by the command line of the process, as the client of the
	 * analysis describes it.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> fromHook(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name,
			SymbolicExpression defaultValue,
			SymbolicExpression dynamic,
			Declared plain,
			ParameterOverrides overrides,
			List<ParameterOverrides.Known> remaining,
			ModelState.Step<A, D, Declared> valued)
			throws SemanticException {
		if (remaining.isEmpty()) {
			ModelState<A, D> result = valued.apply(state, plain);
			if (overrides.othersMayBeOverridden())
				result = result.lub(overridden(state, build, defaultValue, dynamic, valued));
			return result;
		}
		ParameterOverrides.Known known = remaining.get(0);
		List<ParameterOverrides.Known> rest = remaining.subList(1, remaining.size());
		return withField(state, node, NodeModel.FULLY_QUALIFIED,
				(current, fqn) -> current.branch(
						build.and(build.equal(fqn, build.string(known.node())),
								build.equal(name, build.string(known.name()))),
						(matched, c) -> knownValue(matched, build, defaultValue, dynamic, known.value(), valued),
						(other, c) -> fromHook(other, build, node, name, defaultValue, dynamic, plain, overrides,
								rest, valued)));
	}

	/**
	 * A known value given from outside: it replaces the default, and makes a
	 * statically typed declaration fail when its type is not the type of the
	 * default.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> knownValue(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression defaultValue,
			SymbolicExpression dynamic,
			Object value,
			ModelState.Step<A, D, Declared> valued)
			throws SemanticException {
		Predicate<Type> sameKind = sameKind(value);
		Satisfiability anyType = state.satisfies(dynamic);
		boolean mayAgree = anyType != Satisfiability.NOT_SATISFIED || mayBe(state, defaultValue, sameKind);
		boolean mustAgree = anyType == Satisfiability.SATISFIED || certainly(state, defaultValue, sameKind);
		ModelState<A, D> result = state.unreachable();
		if (mayAgree)
			result = result.lub(valued.apply(state, new Declared(build.constant(value), build.bool(false))));
		if (!mustAgree)
			result = result.lub(state.raise(RclpyExceptions.INVALID_PARAMETER_TYPE));
		return result;
	}

	/**
	 * The value of a parameter that may be overridden with an unknown value:
	 * any value of the type of the default, or of any type when the type is
	 * dynamic. A value of another type makes a statically typed declaration
	 * fail.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> overridden(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression defaultValue,
			SymbolicExpression dynamic,
			ModelState.Step<A, D, Declared> valued)
			throws SemanticException {
		Satisfiability anyType = state.satisfies(dynamic);
		Type type = anyType == Satisfiability.NOT_SATISFIED ? typeOf(state, defaultValue) : Untyped.INSTANCE;
		ModelState<A, D> result = valued.apply(state, new Declared(build.unknown(type), build.bool(false)));
		if (anyType != Satisfiability.SATISFIED)
			result = result.lub(state.raise(RclpyExceptions.INVALID_PARAMETER_TYPE));
		return result;
	}

	/**
	 * Continues where the callbacks registered to check parameter values
	 * accept the parameter, and raises where they may reject it.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> checkedBySetCallbacks(
			ModelState<A, D> state,
			SymbolicExpression node,
			ModelState.Step<A, D, SymbolicExpression> accepted)
			throws SemanticException {
		return withField(state, node, SET_CALLBACKS,
				(checked, registered) -> checked.branch(registered,
						(checking, c) -> checking.raise(RclpyExceptions.INVALID_PARAMETER_VALUE)
								.lub(accepted.apply(checking, registered)),
						(unchecked, c) -> accepted.apply(unchecked, registered)));
	}

	/**
	 * Yields the value stored for a default: the default itself when it is a
	 * plain value, unknown otherwise (a list, or a parameter type given
	 * instead of a value).
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> SymbolicExpression storedDefault(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression defaultValue)
			throws SemanticException {
		return certainly(state, defaultValue, ParameterModels::isPlain) ? defaultValue : build.unknown();
	}

	/**
	 * Yields whether a parameter with the given value has none: certainly for
	 * {@code None}, certainly not for other plain values, unknown otherwise (a
	 * parameter type given instead of a value has none).
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> SymbolicExpression notSetOf(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression value)
			throws SemanticException {
		if (certainly(state, value, Type::isNullType))
			return build.bool(true);
		if (certainly(state, value, t -> isPlain(t) && !t.isNullType()))
			return build.bool(false);
		return build.unknown(BoolType.INSTANCE);
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Type typeOf(
			ModelState<A, D> state,
			SymbolicExpression value)
			throws SemanticException {
		Set<Type> types = state.runtimeTypes(value);
		if (types.size() == 1 && isPlain(types.iterator().next()) && !types.iterator().next().isNullType())
			return types.iterator().next();
		return Untyped.INSTANCE;
	}

	private static Predicate<Type> sameKind(
			Object value) {
		if (value instanceof String)
			return Type::isStringType;
		if (value instanceof Boolean)
			return Type::isBooleanType;
		if (value instanceof Integer || value instanceof Long)
			return t -> t.isNumericType() && t.asNumericType().isIntegral();
		return t -> t.isNumericType() && !t.asNumericType().isIntegral();
	}

	/**
	 * Yields whether every type the value may have satisfies a predicate. With
	 * no type information, nothing is certain.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean certainly(
			ModelState<A, D> state,
			SymbolicExpression value,
			Predicate<Type> predicate)
			throws SemanticException {
		Set<Type> types = state.runtimeTypes(value);
		return !types.isEmpty() && types.stream().allMatch(predicate);
	}

	/**
	 * Yields whether some type the value may have satisfies a predicate, or no
	 * type information is available.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean mayBe(
			ModelState<A, D> state,
			SymbolicExpression value,
			Predicate<Type> predicate)
			throws SemanticException {
		Set<Type> types = state.runtimeTypes(value);
		return types.isEmpty() || types.stream().anyMatch(t -> t.isUntyped() || predicate.test(t));
	}

	private static boolean isPlain(
			Type type) {
		return type.isStringType() || type.isNumericType() || type.isBooleanType() || type.isNullType();
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> append(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression name,
			Declared declared,
			SymbolicExpression dynamic,
			SymbolicExpression descriptor)
			throws SemanticException {
		// the descriptor object is stored by rclpy and may be changed later
		// by the program, so its read_only flag is known only without one
		ModelState.Step<A, D, SymbolicExpression> create = (described, readOnly) -> EntityModels.create(described,
				RosTypes.PARAMETER, site,
				(created, entry) -> withField(created, node, PARAMETERS,
						(linked, head) -> linked
								.write(entry, NAME, name)
								.write(entry, VALUE, declared.value())
								.write(entry, TYPE, build.unknown())
								.write(entry, NOT_SET, declared.notSet())
								.write(entry, DYNAMIC, dynamic)
								.write(entry, READ_ONLY, readOnly)
								.write(entry, NEXT, head)
								.write(entry, EntityModels.NODE, node)
								.write(node, PARAMETERS, entry)
								.returning(entry)));
		return state.ifNone(descriptor,
				(plain, d) -> create.apply(plain, build.bool(false)),
				(described, d) -> create.apply(described, build.unknown(BoolType.INSTANCE)));
	}

	/**
	 * Returns a new parameter with the given name and no value, as rclpy
	 * returns for an undeclared parameter that may be read.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> notSet(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression name)
			throws SemanticException {
		return EntityModels.create(state, RosTypes.PARAMETER, site,
				(created, parameter) -> created
						.write(parameter, NAME, name)
						.write(parameter, VALUE, build.none())
						.write(parameter, TYPE, build.unknown())
						.write(parameter, NOT_SET, build.bool(true))
						.write(parameter, DYNAMIC, build.bool(false))
						.returning(parameter));
	}

	/**
	 * Records that the parameters of a node were changed in ways the chain
	 * does not track.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param build the factory of expressions
	 * @param node  a reference to the node
	 *
	 * @return the state after the change
	 *
	 * @throws SemanticException if the change cannot be recorded
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> markChanged(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node)
			throws SemanticException {
		return state.write(node, CHANGED, build.bool(true));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> raiseAny(
			ModelState<A, D> normal,
			ModelState<A, D> state,
			PyExceptionType... types)
			throws SemanticException {
		ModelState<A, D> result = normal;
		for (PyExceptionType type : types)
			result = result.lub(state.raise(type));
		return result;
	}

	/**
	 * Continues where the parameters of a node may have changed in untracked
	 * ways, and where they have not.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> withChanged(
			ModelState<A, D> state,
			SymbolicExpression node,
			ModelState.Step<A, D, SymbolicExpression> unknown,
			ModelState.Step<A, D, SymbolicExpression> known)
			throws SemanticException {
		return withField(state, node, CHANGED, (current, changed) -> current.branch(changed, unknown, known));
	}

	/**
	 * Continues with every value of a field.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> withField(
			ModelState<A, D> state,
			SymbolicExpression reference,
			String field,
			ModelState.Step<A, D, SymbolicExpression> step)
			throws SemanticException {
		return state.forEach(state.read(reference, field).values(), step);
	}
}
