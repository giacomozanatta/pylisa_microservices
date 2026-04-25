package it.unive.pylisa.cfg.expression;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.type.PyClassType;
import it.unive.pylisa.libraries.LibrarySpecificationProvider;
import it.unive.pylisa.symbolic.InternalAttribute;

/**
 * Lowering for Python list literals {@code [a, b, c]} (and the empty literal
 * {@code []}). The lowering allocates a heap region of type
 * {@link LibrarySpecificationProvider#LIST} and writes one cell per element
 * via {@link AccessChild} keyed by the integer index. A dedicated
 * {@link #SIZE_FIELD} cell records the constant length so that downstream
 * consumers (e.g. {@code __len__}, library decorators reading the list) can
 * recover it without iterating the heap fields.
 *
 * <p>
 * Earlier versions of this class folded the literal into a chain of
 * {@code BinaryExpression(ListAppend)} operators over a {@code ListConstant}
 * symbolic value. That approach kept the entire list inside the value lattice
 * (precise only when every element was a {@code ConstantPropagation}
 * constant) and left the heap unaware of the list. The current lowering moves
 * the list into the heap so any value lattice — interval, string-constant,
 * even pluggable per-element domains — can track each cell independently.
 */
public class ListCreation extends NaryExpression {

	/**
	 * Internal-attribute name for the size cell. Wrapped in an
	 * {@link InternalAttribute} so its rendering through
	 * {@link it.unive.lisa.symbolic.value.Constant#toString()} produces the
	 * unquoted token {@code ->size}, mirroring CPython's C-level
	 * {@code ob_size} member of {@code PyVarObject}. See
	 * {@link InternalAttribute} for why this is collision-safe against
	 * user-level container accesses such as {@code m["$size"] = 100}.
	 */
	public static final String SIZE_ATTRIBUTE = "size";

	public ListCreation(
			CFG cfg,
			CodeLocation loc,
			Expression... values) {
		super(cfg, loc, "list", values);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		CodeLocation loc = getLocation();
		if (!PyClassType.isRegistered(LibrarySpecificationProvider.LIST))
			return state;
		Type listType = PyClassType.lookup(LibrarySpecificationProvider.LIST);

		// allocate the heap region for this literal
		MemoryAllocation alloc = new MemoryAllocation(listType, loc);
		AnalysisState<A> sem = interprocedural.getAnalysis().smallStepSemantics(state, alloc, this);

		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression site : sem.getExecution().getComputedExpressions()) {
			HeapReference ref = new HeapReference(listType, site, loc);
			HeapDereference deref = new HeapDereference(listType, ref, loc);

			AnalysisState<A> assign = sem;
			// write each element at its concrete integer index
			for (int i = 0; i < params.length; i++) {
				Constant idx = new Constant(Int32Type.INSTANCE, i, loc);
				AccessChild cellAcc = new AccessChild(Untyped.INSTANCE, deref, idx, loc);
				AnalysisState<A> cellResult = state.bottom();
				for (SymbolicExpression init : params[i]) {
					AnalysisState<A> cellState = interprocedural.getAnalysis()
							.smallStepSemantics(assign, cellAcc, this);
					for (SymbolicExpression cellId : cellState.getExecution().getComputedExpressions())
						cellResult = cellResult
								.lub(interprocedural.getAnalysis().assign(cellState, cellId, init, this));
				}
				assign = cellResult.isBottom() ? assign : cellResult;
			}

			// record the constant length under the analyzer-internal size cell
			Constant lenIdx = new Constant(Untyped.INSTANCE, new InternalAttribute(SIZE_ATTRIBUTE), loc);
			AccessChild lenAcc = new AccessChild(Int32Type.INSTANCE, deref, lenIdx, loc);
			Constant lenVal = new Constant(Int32Type.INSTANCE, params.length, loc);
			AnalysisState<A> lenState = interprocedural.getAnalysis().smallStepSemantics(assign, lenAcc, this);
			AnalysisState<A> lenAssign = state.bottom();
			for (SymbolicExpression lenId : lenState.getExecution().getComputedExpressions())
				lenAssign = lenAssign.lub(interprocedural.getAnalysis().assign(lenState, lenId, lenVal, this));
			AnalysisState<A> finalState = lenAssign.isBottom() ? assign : lenAssign;

			// leave the reference on the stack so callers can pass the list around
			result = result.lub(interprocedural.getAnalysis().smallStepSemantics(finalState, ref, this));
		}

		return result;
	}
}
