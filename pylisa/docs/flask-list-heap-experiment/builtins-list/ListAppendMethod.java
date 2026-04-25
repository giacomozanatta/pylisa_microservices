package it.unive.pylisa.libraries.builtins.list;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.expression.ListCreation;
import it.unive.pylisa.symbolic.InternalAttribute;

/**
 * Lowering for {@code list.append(self, value)}.
 *
 * <p>
 * Mirrors CPython's {@code list.append} at the abstract level by maintaining
 * the {@code $size} cell. Concretely the lowering increments
 * {@code self[->size]} by 1; in CPython this also writes
 * {@code self.ob_item[ob_size++] = value}, but our v0 deliberately does
 * <em>not</em> attempt to write the appended value at the dynamic index
 * {@code self[size]} because doing so soundly requires either a fresh heap
 * abstraction or a value-domain query at lowering time to materialise
 * {@code size} as a {@link Constant}. v1 will do that — for now the value is
 * simply not retained, which is unsound but keeps {@code len(x)} precise (the
 * primary signal needed by Flask-style decorators that read the list shape
 * after construction-and-append).
 *
 * <p>
 * The increment is expressed as a value-level
 * {@link it.unive.lisa.symbolic.value.BinaryExpression} over the size cell's
 * resolved identifier, so the value lattice does the actual arithmetic
 * (e.g. {@code ConstantPropagation} produces {@code 0 + 1 = 1}, an interval
 * lattice produces {@code [n,n] + [1,1] = [n+1,n+1]}, etc.).
 */
public class ListAppendMethod extends it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {

	private Statement st;

	public ListAppendMethod(
			CFG cfg,
			CodeLocation location,
			Expression self,
			Expression value) {
		super(cfg, location, "append", Untyped.INSTANCE, self, value);
	}

	public static ListAppendMethod build(
			CFG cfg,
			CodeLocation location,
			Expression[] exprs) {
		return new ListAppendMethod(cfg, location, exprs[0], exprs[1]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression self,
			SymbolicExpression value,
			StatementStore<A> expressions)
			throws SemanticException {
		CodeLocation loc = getLocation();
		HeapDereference deref = new HeapDereference(Untyped.INSTANCE, self, loc);
		Constant sizeIdx = new Constant(Untyped.INSTANCE, new InternalAttribute(ListCreation.SIZE_ATTRIBUTE), loc);
		AccessChild sizeAccess = new AccessChild(Int32Type.INSTANCE, deref, sizeIdx, loc);

		// resolve the size cell's identifier so we can both read and write it
		AnalysisState<A> sized = interprocedural.getAnalysis().smallStepSemantics(state, sizeAccess, st);

		Constant one = new Constant(Int32Type.INSTANCE, 1, loc);
		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression sizeId : sized.getExecution().getComputedExpressions()) {
			it.unive.lisa.symbolic.value.BinaryExpression plusOne = new it.unive.lisa.symbolic.value.BinaryExpression(
					Int32Type.INSTANCE, sizeId, one, NumericNonOverflowingAdd.INSTANCE, loc);
			result = result.lub(interprocedural.getAnalysis().assign(sized, sizeId, plusOne, st));
		}
		return result.isBottom() ? state : result;
	}
}
