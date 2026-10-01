package it.unive.pylisa.cfg.statement;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.type.PyFunctionType;
import it.unive.pylisa.libraries.natives.LibraryNative;
import java.util.Arrays;

/**
 * A Python call, {@code callee(arguments)}, named after Python's
 * {@code ast.Call}. The callee is the first sub-expression; for a method call
 * written {@code receiver.attribute(arguments)}, the receiver is the second.
 * The call is dispatched on the runtime types of its callee (see
 * {@link CallTargets}).
 */
public class PyCall extends NaryExpression {
	Expression identifier;
	private final boolean hasReceiver;
	/**
	 * Marks call nodes synthesised by the frontend's decorator visitor.
	 * <p>
	 * When this flag is set and the call target's runtime type is unresolved
	 * (e.g. an external decorator like {@code slowapi.Limiter.limit(...)} for
	 * which no library spec exists), {@link #forwardSemanticsAux} treats the
	 * call as a pass-through and propagates the inner argument's value
	 * instead of collapsing to {@code PushAny(Untyped)}. This preserves the
	 * decorated function's {@link PyFunctionType} so an outer route decorator
	 * (e.g. {@code @router.post(...)}) can still resolve the handler.
	 * <p>
	 * Without this flag, every external decorator wrapping a route handler
	 * would need its own pluggable-statement library spec just to be
	 * recognised as transparent.
	 */
	private final boolean decoratorApplication;

	public PyCall(
			CFG cfg,
			CodeLocation location,
			Expression identifier,
			Expression[] params) {
		this(cfg, location, identifier, params, false, false);
	}

	public PyCall(
			CFG cfg,
			CodeLocation location,
			Expression identifier,
			Expression[] params,
			boolean hasReceiver) {
		this(cfg, location, identifier, params, hasReceiver, false);
	}

	public PyCall(
			CFG cfg,
			CodeLocation location,
			Expression identifier,
			Expression[] params,
			boolean hasReceiver,
			boolean decoratorApplication) {
		super(cfg, location, "__call__", prependReceiver(params, identifier));
		this.identifier = identifier;
		this.hasReceiver = hasReceiver;
		this.decoratorApplication = decoratorApplication;
	}

	/**
	 * @return {@code true} when this call was emitted by the decorator
	 *             visitor and should fall back to argument pass-through if
	 *             the target's type is unresolved
	 */
	public boolean isDecoratorApplication() {
		return decoratorApplication;
	}

	@Override
	public String toString() {
		return getSubExpressions()[0] + "("
				+ Arrays.stream(getSubExpressions()).toList().subList(1, getSubExpressions().length) + ")";
	}

	private static Expression[] prependReceiver(
			Expression[] params,
			Expression receiver) {
		Expression[] result = new Expression[params.length + 1];
		result[0] = receiver;
		System.arraycopy(params, 0, result, 1, params.length);
		return result;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		PyCall other = (PyCall) o;
		int cmp = Boolean.compare(hasReceiver, other.hasReceiver);
		if (cmp != 0)
			return cmp;
		cmp = Integer.compare(getSubExpressions().length, other.getSubExpressions().length);
		if (cmp != 0)
			return cmp;
		for (int i = 0; i < getSubExpressions().length; i++) {
			cmp = getSubExpressions()[i].toString().compareTo(other.getSubExpressions()[i].toString());
			if (cmp != 0)
				return cmp;
		}
		// two calls at the same location with identical sub-expressions are
		// the same call site: an identity tiebreaker would make the calls
		// built while analysing (e.g. by an instantiation) distinct sites on
		// every fixpoint iteration, preventing convergence
		return 0;
	}

	/**
	 * Yields the arguments this call passes to the callables it calls: its
	 * sub-expressions after the callee, the receiver of a method call
	 * included, unless every value of the receiver is a module, as in
	 * {@code os.getcwd()}: a function reached through a module is not a
	 * method, and is not passed the module.
	 *
	 * @param <A>       the kind of abstract state
	 * @param <D>       the kind of abstract domain
	 * @param analysis  the analysis
	 * @param state     the state after the evaluation of the sub-expressions
	 * @param receivers the values of the receiver, the first sub-expression
	 *                      after the callee
	 *
	 * @return the arguments
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Expression[] arguments(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			ExpressionSet receivers)
			throws SemanticException {
		boolean passed = !hasReceiver || getSubExpressions().length < 2
				|| CallTargets.receiverPassed(CallTargets.receiverTypes(analysis, state, receivers, this));
		return Arrays.copyOfRange(getSubExpressions(), passed ? 1 : 2, getSubExpressions().length);
	}

	/**
	 * Yields the arguments this call passes to the constructor of a class it
	 * instantiates: its sub-expressions after the callee, without the receiver
	 * through which the class is reached, as in {@code module.Class(x)}.
	 *
	 * @return the arguments
	 */
	Expression[] constructorArguments() {
		int first = hasReceiver && getSubExpressions().length > 1 ? 2 : 1;
		return Arrays.copyOfRange(getSubExpressions(), first, getSubExpressions().length);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottomExecution();
		Expression[] arguments = params.length < 2 ? new Expression[0]
				: arguments(interprocedural.getAnalysis(), state, params[1]);
		for (CallTargets.Target target : CallTargets
				.targets(CallTargets.calleeTypes(interprocedural.getAnalysis(), state, params[0], this)))
			result = result.lub(apply(target, interprocedural, state, params, expressions, arguments));
		return result;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> apply(
			CallTargets.Target target,
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions,
			Expression[] arguments)
			throws SemanticException {
		if (target instanceof CallTargets.Instantiation instantiation) {
			Expression[] constructorArguments = constructorArguments();
			Expression[] classParams = new Expression[constructorArguments.length + 1];
			classParams[0] = getSubExpressions()[0];
			System.arraycopy(constructorArguments, 0, classParams, 1, constructorArguments.length);
			PyInstantiation construction = new PyInstantiation(this.getCFG(), getLocation(), instantiation.type(),
					classParams);
			// errors raised while constructing belong to this call
			construction.setParentStatement(this);
			return construction.forwardSemantics(state, interprocedural, expressions);
		}
		Call call = CallTargets.call(target, this, arguments);
		if (call == null)
			return unknownResult(interprocedural, state, params);
		AnalysisState<A> callResult = call.forwardSemantics(state, interprocedural, expressions);
		// a library model is checked to have a continuation for every
		// reachable input unless its callable may never return, so its result
		// is kept as it is; any other callee with no result (a Python function
		// whose recursion is still being computed, a native that is not such a
		// model) is given an unknown one
		if (!callResult.isBottom() || target instanceof CallTargets.Native natives
				&& LibraryNative.class.isAssignableFrom(natives.implementation()))
			return callResult;
		return unknownResult(interprocedural, state, params);
	}

	/**
	 * Yields the result of a part of this call that cannot be dispatched: an
	 * unknown value. A decorator that cannot be resolved is assumed to return
	 * the function it decorates, so that an outer decorator still sees it.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> unknownResult(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params)
			throws SemanticException {
		if (decoratorApplication && params.length > 1) {
			AnalysisState<A> decorated = state.bottomExecution();
			for (SymbolicExpression function : params[1])
				decorated = decorated.lub(interprocedural.getAnalysis().smallStepSemantics(state, function, this));
			return decorated;
		}
		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(Untyped.INSTANCE, getLocation()),
				this);
	}
}
