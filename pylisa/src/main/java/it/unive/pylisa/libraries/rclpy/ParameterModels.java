package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What the parameter methods of an rclpy node do to the program state.
 * <p>
 * rclpy keeps the declared parameters of a node in a dictionary from their
 * name to a {@code Parameter} object. Here the dictionary is a chain: the
 * field {@value #PARAMETERS} of the node refers to the most recently declared
 * {@code Parameter}, whose field {@value #NEXT} refers to the one declared
 * before it, down to {@code None}. Looking up a name walks the chain and
 * compares names under assumption: where the comparison cannot be decided,
 * both "found here" and "look further" are explored, so the outcome holds for
 * any configured value domain.
 * </p>
 * <p>
 * When the program changes parameters in ways the chain does not track
 * (several at once, or by undeclaring one), the node's field
 * {@value #CHANGED} becomes true, and from then on its parameters are
 * unknown.
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
	 * {@code parameter_overrides}.
	 */
	static final String NO_PROGRAM_OVERRIDES = "$no_parameter_overrides";

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
	 * The field of a parameter that tells whether it has a value (a parameter
	 * declared with only its type has none until it is set).
	 */
	static final String INITIALIZED = "$initialized";

	private static final String NAME = "name";

	private static final String VALUE = "value";

	private static final String TYPE = "type_";

	private ParameterModels() {
	}

	/**
	 * Initializes the parameters of a new node: none is declared.
	 *
	 * @param <A>                the kind of abstract state
	 * @param <D>                the kind of abstract domain
	 * @param state              the state
	 * @param build              the factory of expressions
	 * @param node               a reference to the node
	 * @param programOverrides   the {@code parameter_overrides} argument
	 * @param allowUndeclared    the {@code allow_undeclared_parameters}
	 *                               argument
	 * @param declareFromOverrides the
	 *                               {@code automatically_declare_parameters_from_overrides}
	 *                               argument
	 *
	 * @return the state after the initialization
	 *
	 * @throws SemanticException if the fields cannot be written
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> initialize(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression programOverrides,
			SymbolicExpression allowUndeclared,
			SymbolicExpression declareFromOverrides)
			throws SemanticException {
		// parameters declared automatically from overrides have names the
		// analysis does not know
		ModelState<A, D> initialized = state.write(node, PARAMETERS, build.none())
				.write(node, CHANGED, declareFromOverrides)
				.write(node, ALLOW_UNDECLARED, allowUndeclared);
		return initialized.ifNone(programOverrides,
				(none, v) -> none.write(node, NO_PROGRAM_OVERRIDES, build.bool(true)),
				(given, v) -> given.write(node, NO_PROGRAM_OVERRIDES, build.bool(false)));
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
		ModelState.Step<A, D, SymbolicExpression> declareNew = (fresh, none) -> value(fresh, build, node, name,
				defaultValue, ignoreOverride,
				(valued, value) -> readOnly(valued, build, descriptor,
						(described, readOnly) -> append(described, build, site, node, name, value, readOnly,
								initialized(described, build, defaultValue))));
		return withChanged(state, node,
				(unknown, c) -> unknown.raise(RclpyExceptions.PARAMETER_ALREADY_DECLARED)
						.lub(declareNew.apply(unknown, build.none())),
				(known, c) -> lookup(known, build, node, name,
						(duplicate, entry) -> duplicate.raise(RclpyExceptions.PARAMETER_ALREADY_DECLARED),
						declareNew));
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
						.lub(unknown.raise(RclpyExceptions.PARAMETER_NOT_DECLARED)),
				(known, c) -> lookup(known, build, node, name,
						(found, entry) -> requireInitialized(found, entry,
								(set, e) -> set.returning(entry)),
						(missing, none) -> withField(missing, node, ALLOW_UNDECLARED,
								(checked, allowed) -> checked.branch(allowed,
										(allowing, c1) -> notSet(allowing, build, site, name),
										(forbidding, c1) -> forbidding
												.raise(RclpyExceptions.PARAMETER_NOT_DECLARED)))));
	}

	/**
	 * Reads a parameter or yields an alternative, as
	 * {@code Node.get_parameter_or(name, alternative_value)} does.
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
						(found, entry) -> withField(found, entry, INITIALIZED,
								(checked, initialized) -> checked.branch(initialized,
										(set, c1) -> set.returning(entry),
										(unset, c1) -> otherwise.apply(unset, build.none()))),
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
				(unknown, c) -> markChanged(unknown, build, node)
						.lub(unknown.raise(RclpyExceptions.PARAMETER_NOT_DECLARED))
						.lub(unknown.raise(RclpyExceptions.PARAMETER_IMMUTABLE)),
				(known, c) -> lookup(known, build, node, name,
						(found, entry) -> withField(found, entry, READ_ONLY,
								(checked, readOnly) -> checked.branch(readOnly,
										(immutable, c1) -> immutable.raise(RclpyExceptions.PARAMETER_IMMUTABLE),
										(mutable, c1) -> markChanged(mutable, build, node))),
						(missing, none) -> missing.raise(RclpyExceptions.PARAMETER_NOT_DECLARED)));
	}

	/**
	 * Records that the parameters of a node were changed in ways the chain
	 * does not track, as setting several parameters at once does, and yields
	 * an unknown result.
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
		ModelState<A, D> changed = state.write(node, CHANGED, build.bool(true));
		return changed.returning(build.unknown())
				.lub(changed.raise(RclpyExceptions.PARAMETER_NOT_DECLARED))
				.lub(changed.raise(RclpyExceptions.PARAMETER_ALREADY_DECLARED))
				.lub(changed.raise(RclpyExceptions.INVALID_PARAMETER_TYPE))
				.lub(changed.raise(RclpyExceptions.PARAMETER_IMMUTABLE));
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
		ModelState<A, D> named = state.write(self, NAME, name)
				.write(self, VALUE, value)
				.write(self, INITIALIZED, build.bool(true));
		return named.ifNone(type,
				(inferred, c) -> inferred.write(self, TYPE, build.unknown()).returning(build.none()),
				(given, c) -> given.write(self, TYPE, type).returning(build.none()));
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
		Set<Type> types = state.runtimeTypes(link);
		if (types.isEmpty() || types.stream().anyMatch(t -> t.isNullType() || t.isUntyped()))
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
	 * Computes the value a declared parameter gets: a value given from
	 * outside the program if there may be one, the default otherwise.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> value(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name,
			SymbolicExpression defaultValue,
			SymbolicExpression ignoreOverride,
			ModelState.Step<A, D, SymbolicExpression> valued)
			throws SemanticException {
		SymbolicExpression stored = storedDefault(state, build, defaultValue);
		ParameterOverrides overrides = ParameterOverrides.current();
		return state.branch(ignoreOverride,
				(ignored, c) -> valued.apply(ignored, stored),
				(considered, c) -> known(considered, build, node, name, defaultValue, stored, overrides,
						overrides.known(), valued));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> known(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name,
			SymbolicExpression defaultValue,
			SymbolicExpression stored,
			ParameterOverrides overrides,
			List<ParameterOverrides.Known> remaining,
			ModelState.Step<A, D, SymbolicExpression> valued)
			throws SemanticException {
		if (remaining.isEmpty())
			return unlisted(state, build, node, defaultValue, stored, overrides, valued);
		ParameterOverrides.Known override = remaining.get(0);
		List<ParameterOverrides.Known> rest = remaining.subList(1, remaining.size());
		return withField(state, node, NodeModel.FULLY_QUALIFIED,
				(current, fqn) -> current.branch(
						build.and(build.equal(fqn, build.string(override.node())),
								build.equal(name, build.string(override.name()))),
						(overridden, c) -> valued.apply(overridden, build.constant(override.value())),
						(other, c) -> known(other, build, node, name, defaultValue, stored, overrides, rest,
								valued)));
	}

	/**
	 * The value of a parameter that no known override matches: it may still
	 * be overridden if the client of the analysis says others may be, or if
	 * the program itself gives the node overrides.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> unlisted(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression defaultValue,
			SymbolicExpression stored,
			ParameterOverrides overrides,
			ModelState.Step<A, D, SymbolicExpression> valued)
			throws SemanticException {
		if (overrides.othersMayBeOverridden())
			return overridden(state, build, defaultValue, valued);
		return withField(state, node, NO_PROGRAM_OVERRIDES,
				(current, none) -> current.branch(none,
						(plain, c) -> valued.apply(plain, stored),
						(given, c) -> overridden(given, build, defaultValue, valued)));
	}

	/**
	 * The value of a parameter that may be overridden: any value of the type
	 * of the default. A value of another type makes a statically typed
	 * declaration fail.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> overridden(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression defaultValue,
			ModelState.Step<A, D, SymbolicExpression> valued)
			throws SemanticException {
		ModelState<A, D> result = valued.apply(state, build.unknown(typeOf(state, defaultValue)));
		return result.lub(state.ifNone(defaultValue,
				(dynamic, c) -> dynamic.unreachable(),
				(typed, c) -> typed.raise(RclpyExceptions.INVALID_PARAMETER_TYPE)));
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
		Set<Type> types = state.runtimeTypes(defaultValue);
		return !types.isEmpty() && types.stream().allMatch(ParameterModels::isPlain) ? defaultValue
				: build.unknown();
	}

	/**
	 * Yields whether a parameter declared with the given default has a value:
	 * a parameter declared with only a type has none.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> SymbolicExpression initialized(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression defaultValue)
			throws SemanticException {
		Set<Type> types = state.runtimeTypes(defaultValue);
		return !types.isEmpty() && types.stream().allMatch(ParameterModels::isPlain) ? build.bool(true)
				: build.unknown(BoolType.INSTANCE);
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

	private static boolean isPlain(
			Type type) {
		return type.isStringType() || type.isNumericType() || type.isBooleanType() || type.isNullType();
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> readOnly(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression descriptor,
			ModelState.Step<A, D, SymbolicExpression> described)
			throws SemanticException {
		return state.ifNone(descriptor,
				(plain, c) -> described.apply(plain, build.bool(false)),
				(given, c) -> withField(given, descriptor, "read_only", described));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> append(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression name,
			SymbolicExpression value,
			SymbolicExpression readOnly,
			SymbolicExpression initialized)
			throws SemanticException {
		return EntityModels.create(state, RosTypes.PARAMETER, site,
				(created, entry) -> withField(created, node, PARAMETERS,
						(linked, head) -> linked
								.write(entry, NAME, name)
								.write(entry, VALUE, value)
								.write(entry, TYPE, build.unknown())
								.write(entry, READ_ONLY, readOnly)
								.write(entry, INITIALIZED, initialized)
								.write(entry, NEXT, head)
								.write(entry, EntityModels.NODE, node)
								.write(node, PARAMETERS, entry)
								.returning(entry)));
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
						.write(parameter, INITIALIZED, build.bool(false))
						.returning(parameter));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> requireInitialized(
			ModelState<A, D> state,
			SymbolicExpression entry,
			ModelState.Step<A, D, SymbolicExpression> initialized)
			throws SemanticException {
		return withField(state, entry, INITIALIZED,
				(checked, flag) -> checked.branch(flag,
						initialized,
						(unset, c) -> unset.raise(RclpyExceptions.PARAMETER_UNINITIALIZED)));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> markChanged(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node)
			throws SemanticException {
		return state.write(node, CHANGED, build.bool(true)).returning(build.none());
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
