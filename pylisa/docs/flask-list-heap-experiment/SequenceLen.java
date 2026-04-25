package it.unive.pylisa.libraries;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.expression.ListCreation;
import it.unive.pylisa.symbolic.InternalAttribute;

/**
 * Lowering for {@code __len__(seq)} on Python sequences.
 * <p>
 * Resolves to {@code AccessChild(deref(seq), Constant(InternalAttribute("size")))}
 * so that containers initialised by {@link ListCreation} (and other lowerings
 * that bind the size cell) yield a precise integer length when one was
 * statically known. The internal cell is keyed by an
 * {@link InternalAttribute} value rather than a plain string, which keeps the
 * resulting heap identifier — rendered as {@code pp@loc[->size]} — disjoint
 * from any user-level key that resolves through a string {@code Constant}.
 * If no size cell was ever written — e.g. for receivers we can't prove are
 * Python sequences — the heap returns an empty rewrite and we fall back to
 * {@link PushAny} to keep the analysis reachable.
 */
public class SequenceLen extends UnaryExpression implements PluggableStatement {

	protected Statement st;

	protected SequenceLen(
			CFG cfg,
			CodeLocation location,
			String constructName,
			Expression sequence) {
		super(cfg, location, constructName, sequence);
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
		Constant lenIdx = new Constant(Untyped.INSTANCE,
				new InternalAttribute(ListCreation.SIZE_ATTRIBUTE), loc);
		AccessChild access = new AccessChild(Int32Type.INSTANCE, deref, lenIdx, loc);
		AnalysisState<A> heapResult = interprocedural.getAnalysis().smallStepSemantics(state, access, st);
		if (heapResult.isBottom())
			return interprocedural.getAnalysis().smallStepSemantics(state,
					new PushAny(Int32Type.INSTANCE, loc), st);
		return heapResult;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	public static SequenceLen build(
			CFG cfg,
			CodeLocation location,
			Expression[] exprs) {
		return new SequenceLen(cfg, location, "__len__", exprs[0]);
	}

	@Override
	final public void setOriginatingStatement(
			Statement st) {
		this.st = st;
	}
}
