package it.unive.pylisa.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.NativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.type.PyClassType;
import it.unive.pylisa.cfg.type.PyFunctionType;
import it.unive.pylisa.libraries.loader.LibraryNativeCFG;
import it.unive.pylisa.program.language.parameterassignment.ArgumentBinding;
import it.unive.pylisa.program.type.NoInfoType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The targets a call resolves to in a given state, as the analysis itself
 * dispatches the call. Every runtime type of the callee either yields a
 * target or is reported as an {@link Unresolved} part, so that no execution
 * of the call is silently dropped: the analysis continues unresolved parts
 * with an unknown result, and readers of the results see them.
 */
public final class CallTargets {

	/**
	 * Beyond this many runtime types, a callee is not dispatched type by type.
	 */
	private static final int TYPE_LIMIT = 20;

	/**
	 * A target of a call.
	 */
	public sealed interface Target permits Native, Python, Instantiation, Unresolved {
	}

	/**
	 * A callable declared in a library specification.
	 *
	 * @param cfg            the code of the callable
	 * @param implementation the class modelling the callable
	 * @param library        the library declaring the callable
	 */
	public record Native(LibraryNativeCFG cfg, Class<? extends NaryExpression> implementation, String library)
			implements
			Target {
	}

	/**
	 * A function whose Python code is analysed.
	 *
	 * @param cfg the code of the function
	 */
	public record Python(CFG cfg) implements Target {
	}

	/**
	 * The construction of an instance of a class, with an unresolved part for
	 * each part of {@code __new__} or {@code __init__} that cannot be
	 * dispatched.
	 *
	 * @param type           the class
	 * @param creation       the targets of {@code __new__}
	 * @param initialization the targets of {@code __init__}
	 */
	public record Instantiation(PyClassType type, List<Target> creation, List<Target> initialization)
			implements
			Target {
	}

	/**
	 * A part of the call that cannot be dispatched: the analysis continues it
	 * with an unknown result.
	 *
	 * @param reason why the part cannot be dispatched
	 */
	public record Unresolved(String reason) implements Target {
	}

	private CallTargets() {
	}

	/**
	 * Yields the targets of a call, as stored in the results of the analysis
	 * of its CFG: the callee is read after the evaluation of the callee
	 * expression, and its types in the state after all the sub-expressions,
	 * which is the state the call is applied to.
	 *
	 * @param <A>      the kind of abstract state
	 * @param <D>      the kind of abstract domain
	 * @param analysis the analysis
	 * @param result   the results of the CFG of the call, in one context
	 * @param call     the call
	 *
	 * @return the targets; empty if no execution applies the call
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> List<Target> of(
			Analysis<A, D> analysis,
			AnalyzedCFG<A> result,
			FunctionApply call)
			throws SemanticException {
		Expression[] sub = call.getSubExpressions();
		AnalysisState<A> applied = result.getAnalysisStateAfter(sub[sub.length - 1]);
		if (applied.getExecution().isBottom() || applied.getExecutionState().isBottom())
			return List.of();
		ExpressionSet callees = result.getAnalysisStateAfter(sub[0]).getExecutionExpressions();
		return of(analysis, applied, callees, call);
	}

	/**
	 * The arguments of a call bound to one formal parameter of a callee.
	 *
	 * @param formal    the formal parameter
	 * @param arguments the arguments bound to it: one for a plain parameter,
	 *                      any number for a {@code *args} or {@code **kw}
	 *                      parameter, none for a parameter that takes its
	 *                      default
	 */
	public record Binding(Parameter formal, List<Expression> arguments) {
	}

	/**
	 * Binds the arguments of a call to the formal parameters of one of its
	 * targets, as stored in the results of the analysis of its CFG and as the
	 * dispatch passes them (see {@link ArgumentBinding}): a callable receives
	 * {@link FunctionApply#arguments}; the {@code __init__} of an
	 * instantiation receives the object being created and then the
	 * constructor arguments, and its first parameter, bound to the object, is
	 * left out.
	 *
	 * @param <A>            the kind of abstract state
	 * @param <D>            the kind of abstract domain
	 * @param analysis       the analysis
	 * @param result         the results of the CFG of the call, in one context
	 * @param call           the call, applied in that context
	 * @param callee         the target, or the {@code __init__} of an
	 *                           instantiation
	 * @param initialization whether {@code callee} is the {@code __init__} of
	 *                           an instantiation
	 *
	 * @return the binding of each formal parameter, in order; empty when the
	 *             arguments do not match the parameters
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Optional<List<Binding>> bind(
			Analysis<A, D> analysis,
			AnalyzedCFG<A> result,
			FunctionApply call,
			CodeMember callee,
			boolean initialization)
			throws SemanticException {
		Parameter[] formals = callee.getDescriptor().getFormals();
		Expression[] actuals;
		if (initialization) {
			if (formals.length == 0)
				return Optional.empty();
			formals = Arrays.copyOfRange(formals, 1, formals.length);
			actuals = call.constructorArguments();
		} else {
			Expression[] sub = call.getSubExpressions();
			actuals = sub.length < 2 ? new Expression[0]
					: call.arguments(analysis, result.getAnalysisStateAfter(sub[sub.length - 1]),
							result.getAnalysisStateAfter(sub[1]).getExecutionExpressions());
		}
		Optional<List<List<Integer>>> positions = ArgumentBinding.bind(formals, actuals);
		if (positions.isEmpty())
			return Optional.empty();
		List<Binding> bindings = new ArrayList<>();
		for (int i = 0; i < formals.length; i++)
			bindings.add(new Binding(formals[i],
					positions.get().get(i).stream().map(position -> actuals[position]).toList()));
		return Optional.of(bindings);
	}

	/**
	 * Yields the targets of a call in the state it is applied to.
	 *
	 * @param <A>      the kind of abstract state
	 * @param <D>      the kind of abstract domain
	 * @param analysis the analysis
	 * @param state    the state after the evaluation of every sub-expression
	 *                     of the call
	 * @param callees  the values of the callee expression
	 * @param point    the program point of the call
	 *
	 * @return the targets, with an unresolved part for everything that cannot
	 *             be dispatched
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> List<Target> of(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			ExpressionSet callees,
			ProgramPoint point)
			throws SemanticException {
		List<Target> targets = new ArrayList<>();
		for (SymbolicExpression callee : callees)
			targets.addAll(ofCallee(analysis, state, callee, point));
		if (targets.isEmpty())
			targets.add(new Unresolved("the callee has no value"));
		return targets;
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> List<Target> ofCallee(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression callee,
			ProgramPoint point)
			throws SemanticException {
		List<Target> targets = new ArrayList<>();
		if (callee instanceof PushAny)
			return List.of(new Unresolved("the callee is unknown"));
		Set<Type> types = analysis.getRuntimeTypesOf(state, callee, point);
		if (types.isEmpty() || types.stream().allMatch(NoInfoType.INSTANCE::equals)) {
			// the state may not carry the type of a library global referred
			// to by its qualified name: the registered type is used, but the
			// analysis did not derive it
			types = new HashSet<>();
			if (callee instanceof GlobalVariable global)
				types.addAll(registeredTypes(global.getName()));
			targets.add(new Unresolved("the type of the callee is not known to the analysis"));
		}
		if (types.size() > TYPE_LIMIT) {
			Set<Type> callable = new HashSet<>();
			for (Type type : types)
				if (type instanceof PyFunctionType || type instanceof PyClassType)
					callable.add(type);
			if (callable.isEmpty() || callable.size() > TYPE_LIMIT)
				return List.of(new Unresolved("the callee has more than " + TYPE_LIMIT + " runtime types"));
			targets.add(new Unresolved("runtime types of the callee beyond the limit are not callable"));
			types = callable;
		}
		for (Type type : types)
			if (type instanceof PyClassType classType)
				targets.add(new Instantiation(classType,
						methodTargets(attribute(analysis, state, classType, "__new__", point), "__new__", classType),
						methodTargets(attribute(analysis, state, classType, "__init__", point), "__init__",
								classType)));
			else if (type instanceof PyFunctionType function)
				targets.add(function(function));
			else if (NoInfoType.INSTANCE.equals(type))
				targets.add(new Unresolved("the callee may have a type the analysis does not know"));
			else
				targets.add(new Unresolved("the callee may be a " + describe(type) + ", which is not dispatched"));
		return targets;
	}

	private static Set<Type> registeredTypes(
			String name) {
		String qualified = name.startsWith("$") ? name.substring(1).replace("::", ".") : name;
		if (PyFunctionType.isRegistered(qualified))
			return Set.of(PyFunctionType.lookup(qualified));
		// conditional class redefinitions share a qualified name: all of them
		// are candidates
		return new HashSet<>(PyClassType.lookupAllByBaseName(qualified));
	}

	private static String describe(
			Type type) {
		return type instanceof ReferenceType reference ? "reference to " + reference.getInnerType() : type.toString();
	}

	/**
	 * Yields the target of calling a function.
	 *
	 * @param function the type of the function
	 *
	 * @return the target
	 */
	static Target function(
			PyFunctionType function) {
		CodeMember code = function.getUnit().getFunction();
		if (code instanceof LibraryNativeCFG cfg)
			return new Native(cfg, cfg.getImplementation(), cfg.getLibrary());
		if (code instanceof CFG cfg)
			return new Python(cfg);
		return new Unresolved("the function " + function + " has no code");
	}

	/**
	 * Yields the targets of a method of a class, as resolved by
	 * {@link #attribute}: a function target for each function, and an
	 * unresolved part for a method that is not found, found only by its name,
	 * or that may not be a function.
	 *
	 * @param resolution the resolution of the method
	 * @param method     the name of the method
	 * @param classType  the class
	 *
	 * @return the targets
	 */
	static List<Target> methodTargets(
			Resolution resolution,
			String method,
			PyClassType classType) {
		List<Target> targets = new ArrayList<>();
		if (resolution.types().isEmpty())
			targets.add(new Unresolved("no " + method + " of " + classType + " is found (" + resolution.mode() + ")"));
		if (resolution.mode().endsWith("-registry"))
			targets.add(new Unresolved(method + " of " + classType + " is found only by its name"));
		for (Type type : resolution.types())
			if (type instanceof PyFunctionType function)
				targets.add(function(function));
			else if (NoInfoType.INSTANCE.equals(type))
				targets.add(
						new Unresolved(method + " of " + classType + " may have a type the analysis does not know"));
			else
				targets.add(new Unresolved(method + " of " + classType + " may be a " + describe(type)));
		return targets;
	}

	/**
	 * Builds the synthetic call that applies a function target at a call
	 * site, linked to the site so that its errors belong to it.
	 *
	 * @param target    the target
	 * @param site      the call site
	 * @param arguments the argument expressions
	 *
	 * @return the call, or {@code null} if the target is not a function
	 */
	static Call call(
			Target target,
			Statement site,
			Expression[] arguments) {
		Call call;
		if (target instanceof Native natives)
			call = new NativeCall(site.getCFG(), site.getLocation(), Call.CallType.STATIC, "", "$call",
					List.of(natives.cfg()), arguments);
		else if (target instanceof Python python)
			call = new CFGCall(site.getCFG(), site.getLocation(), Call.CallType.STATIC, "", "$call",
					List.of(python.cfg()), arguments);
		else
			return null;
		call.setParentStatement(site);
		return call;
	}

	/**
	 * The resolution of an attribute of a class through its ancestors.
	 *
	 * @param owner      the class defining the attribute, or {@code null} if
	 *                       none does
	 * @param types      the runtime types of the attribute, empty if it is not
	 *                       found
	 * @param lookupPath the classes visited, in order
	 * @param mode       how the attribute was found
	 */
	record Resolution(CompilationUnit owner, Set<Type> types, String lookupPath, String mode) {
	}

	/**
	 * Resolves an attribute of a class as Python does for a single chain of
	 * ancestors: the first class defining it wins, whether or not its value is
	 * callable, and all the types of its value are kept, including a type the
	 * analysis does not know. Classes with more than one direct ancestor are
	 * not supported, and their attributes are not found.
	 *
	 * @param <A>       the kind of abstract state
	 * @param <D>       the kind of abstract domain
	 * @param analysis  the analysis
	 * @param state     the state the attribute is read in
	 * @param classType the class
	 * @param attribute the name of the attribute
	 * @param point     the program point reading it
	 *
	 * @return the resolution
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Resolution attribute(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			PyClassType classType,
			String attribute,
			ProgramPoint point)
			throws SemanticException {
		LinkedHashSet<String> visited = new LinkedHashSet<>();
		ArrayDeque<CompilationUnit> work = new ArrayDeque<>();
		work.add(classType.getUnit());
		while (!work.isEmpty()) {
			CompilationUnit current = work.removeFirst();
			if (!visited.add(current.getName()))
				continue;
			GlobalVariable variable = new GlobalVariable(Untyped.INSTANCE,
					"$" + current.getName() + "::" + attribute, point.getLocation());
			Set<Type> types = analysis.getRuntimeTypesOf(state, variable, point);
			String inherited = visited.size() == 1 ? "direct" : "inherited";
			if (types.stream().anyMatch(type -> !NoInfoType.INSTANCE.equals(type)))
				return new Resolution(current, types, String.join(" -> ", visited), inherited);
			// the state may not carry the binding of a library method in deep
			// contexts: a library method has a stable qualified name
			String qualified = current.getName() + "." + attribute;
			if (PyFunctionType.isRegistered(qualified))
				return new Resolution(current, Set.of(PyFunctionType.lookup(qualified)),
						String.join(" -> ", visited), inherited + "-registry");
			Collection<CompilationUnit> ancestors = current.getImmediateAncestors();
			if (ancestors.size() > 1)
				return new Resolution(null, Set.of(), String.join(" -> ", visited), "unsupported-multiple-ancestors");
			work.addAll(ancestors);
		}
		return new Resolution(null, Set.of(), String.join(" -> ", visited), "unresolved");
	}
}
