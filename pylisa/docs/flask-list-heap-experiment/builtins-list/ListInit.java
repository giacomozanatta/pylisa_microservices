package it.unive.pylisa.libraries.builtins.list;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.expression.ListCreation;
import it.unive.pylisa.symbolic.InternalAttribute;

/**
 * Lowering for {@code list.__init__(self)} (and {@code list()} no-arg
 * construction). Initialises the analyzer-internal {@code $size} cell on the
 * receiver allocation to {@code 0}, mirroring CPython's
 * {@code list.__init__} which allocates an empty {@code ob_item} array and
 * sets {@code ob_size = 0}.
 *
 * <p>
 * Without this binding, calling {@code x = list()} produces a heap region
 * with no size cell — subsequent {@code len(x)} reads return TOP because the
 * cell read fails. Initialising {@code $size} here makes the empty-list case
 * symmetric with the literal {@code []} path emitted by {@link ListCreation},
 * both of which leave the receiver in the same shape: an allocation with
 * {@code [->size] = 0} and no element cells.
 */
public class ListInit extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {

	private Statement st;

	public ListInit(
			CFG cfg,
			CodeLocation location,
			Expression self) {
		super(cfg, location, "__init__", Untyped.INSTANCE, self);
	}

	public static ListInit build(
			CFG cfg,
			CodeLocation location,
			Expression[] exprs) {
		return new ListInit(cfg, location, exprs[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	final public void setOriginatingStatement(
			Statement st) {
		this.st = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		CodeLocation loc = getLocation();
		HeapDereference deref = new HeapDereference(Untyped.INSTANCE, expr, loc);
		Constant sizeIdx = new Constant(Untyped.INSTANCE, new InternalAttribute(ListCreation.SIZE_ATTRIBUTE), loc);
		AccessChild sizeAccess = new AccessChild(Int32Type.INSTANCE, deref, sizeIdx, loc);
		Constant zero = new Constant(Int32Type.INSTANCE, 0, loc);

		AnalysisState<A> readState = interprocedural.getAnalysis().smallStepSemantics(state, sizeAccess, st);
		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression sizeId : readState.getExecution().getComputedExpressions())
			result = result.lub(interprocedural.getAnalysis().assign(readState, sizeId, zero, st));
		return result.isBottom() ? state : result;
	}
}
