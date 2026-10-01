package it.unive.pylisa.cfg.statement;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.*;
import it.unive.pylisa.cfg.type.PyClassType;
import it.unive.pylisa.debug.ConstructorResolutionTrace;
import java.util.ArrayList;
import java.util.List;

public class ClassInstantiation extends NaryExpression {

	private final PyClassType classType;

	public ClassInstantiation(
			CFG cfg,
			CodeLocation location,
			PyClassType classType,
			Expression[] params) {
		super(cfg, location, "$ClassInstantiation", classType, params);
		this.classType = classType;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottomExecution();
		ExpressionSet objects = new ExpressionSet().bottom();
		CallTargets.Resolution creation = CallTargets.attribute(interprocedural.getAnalysis(), state, classType,
				"__new__", this);
		recordResolution(classType, "__new__", creation);
		for (CallTargets.Target target : CallTargets.methodTargets(creation, "__new__", classType)) {
			Call call = CallTargets.call(target, this, getSubExpressions());
			if (call == null) {
				// a part of __new__ that cannot be dispatched creates an
				// unknown object
				AnalysisState<A> unknown = unknownObject(interprocedural, state);
				result = result.lub(unknown);
				objects = objects.lub(unknown.getExecutionExpressions());
				continue;
			}
			AnalysisState<A> created = call.forwardSemantics(state, interprocedural, expressions);
			boolean reachable = !created.getExecution().isBottom() && !created.getExecutionState().isBottom();
			if (reachable && created.getExecutionExpressions().isEmpty()) {
				// __new__ returns normally without a value: the object is
				// unknown
				AnalysisState<A> unknown = unknownObject(interprocedural, created);
				result = result.lub(unknown);
				objects = objects.lub(unknown.getExecutionExpressions());
			}
			// the object exists once __init__ returns: an __init__ that always
			// raises leaves no execution after it, and what __init__ returns is
			// not the object
			AnalysisState<A> initialized = created.bottomExecution();
			for (SymbolicExpression e : created.getExecutionExpressions())
				if (e instanceof Identifier) {
					objects = objects.lub(new ExpressionSet(e));
					initialized = initialized.lub(initialize(interprocedural, created, expressions));
				} else {
					// __init__ cannot be run on an object that is not a named
					// location: the object is unknown
					AnalysisState<A> unknown = unknownObject(interprocedural, created);
					initialized = initialized.lub(unknown);
					objects = objects.lub(unknown.getExecutionExpressions());
				}
			result = result.lub(initialized);
		}
		return result.withExecutionExpressions(objects);
	}

	/**
	 * Runs {@code __init__} on the object just created, with the arguments of
	 * this instantiation. {@code __init__} is looked up after {@code __new__}
	 * has run, as Python does, so a {@code __new__} that rebinds it is taken
	 * into account.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> initialize(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> created,
			StatementStore<A> expressions)
			throws SemanticException {
		CallTargets.Resolution initialization = CallTargets.attribute(interprocedural.getAnalysis(), created,
				classType, "__init__", this);
		CompilationUnit owner = initialization.owner() == null ? classType.getUnit() : initialization.owner();
		PythonScopedAttributeAccessRef init = new PythonScopedAttributeAccessRef(this.getCFG(), getLocation(), owner,
				new Global(getLocation(), owner, "__init__", false));
		Expression[] arguments = new Expression[getSubExpressions().length];
		arguments[0] = new InstrumentedReceiverRef(this.getCFG(), getLocation(), false);
		for (int i = 1; i < getSubExpressions().length; i++)
			arguments[i] = getSubExpressions()[i];
		FunctionApply call = new FunctionApply(this.getCFG(), getLocation(), init, arguments);
		// errors raised by __init__ belong to this instantiation
		call.setParentStatement(this);
		return call.forwardSemantics(created, interprocedural, expressions);
	}

	/**
	 * Yields the state where the created object is unknown.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> unknownObject(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state)
			throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(Untyped.INSTANCE, getLocation()),
				this);
	}

	private void recordResolution(
			PyClassType classType,
			String attribute,
			CallTargets.Resolution resolution) {
		List<String> ancestors = new ArrayList<>();
		for (CompilationUnit ancestor : classType.getUnit().getImmediateAncestors())
			ancestors.add(ancestor.getName());
		ConstructorResolutionTrace.record(
				getLocation().toString(),
				classType.getUnit().getName(),
				ancestors,
				attribute,
				resolution.lookupPath(),
				resolution.types().toString(),
				resolution.owner() == null ? "<none>" : resolution.owner().getName(),
				resolution.mode());
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		ClassInstantiation other = (ClassInstantiation) o;
		int cmp = classType.toString().compareTo(other.classType.toString());
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
		// Same reasoning as FunctionApply: identity hash as tiebreaker prevents
		// convergence when ClassInstantiation objects are dynamically created
		// per
		// fixpoint iteration at the same code location with identical
		// parameters.
		return 0;
	}
}
