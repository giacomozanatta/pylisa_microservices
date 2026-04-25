package it.unive.pylisa.libraries;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.type.PyClassType;
import it.unive.pylisa.symbolic.operators.dataframes.Iterate;
import java.util.Set;

/**
 * Lowering for {@code __getitem__(seq, index)} on Python sequences.
 * <p>
 * The dispatch fans out by the runtime type of the receiver:
 * <ul>
 * <li>pandas {@code DataFrame} → existing {@link Iterate} column-projection
 * shortcut (untouched).</li>
 * <li>everything else → emit {@code AccessChild(deref(seq), index)} so that
 * the heap domain produces a per-cell allocation site whose value is tracked
 * by the underlying value lattice. This replaces the previous
 * {@link PushAny}-everything fallback, giving real precision for list and
 * tuple literals like {@code methods=["GET","POST"]}; the index is
 * propagated as-is, so a constant index resolves to a strong site, while a
 * non-constant index goes through the heap's carve-out sentinel (see
 * {@code PyFieldSensitivePointBasedHeap}).</li>
 * </ul>
 */
public class SequenceGetItem extends BinaryExpression implements PluggableStatement {

	protected Statement st;

	protected SequenceGetItem(
			CFG cfg,
			CodeLocation location,
			String constructName,
			Expression sequence,
			Expression index) {
		super(cfg, location, constructName, sequence, index);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		CodeLocation loc = getLocation();
		try {
			PyClassType dftype = PyClassType.lookup(LibrarySpecificationProvider.PANDAS_DF);
			Type dfref = dftype.getReference();
			PyClassType seriestype = PyClassType.lookup(LibrarySpecificationProvider.PANDAS_SERIES);
			Set<Type> rts = interprocedural.getAnalysis().getRuntimeTypesOf(state, left, this);
			if (rts != null && rts.stream().anyMatch(dfref::equals)) {
				HeapDereference deref = new HeapDereference(dftype, left, loc);
				UnaryExpression iterate = new UnaryExpression(seriestype, deref, new Iterate(0), loc);
				return interprocedural.getAnalysis().smallStepSemantics(state, iterate, st);
			}
		} catch (Exception ignored) {
			// fall through to the generic AccessChild path
		}

		// Generic case: produce AccessChild(deref(seq), index) and let the
		// heap domain turn it into a value-domain identifier whose value is
		// whatever was written to that cell at literal-construction time.
		HeapDereference deref = new HeapDereference(Untyped.INSTANCE, left, loc);
		AccessChild access = new AccessChild(Untyped.INSTANCE, deref, right, loc);
		return interprocedural.getAnalysis().smallStepSemantics(state, access, st);
	}

	public static SequenceGetItem build(
			CFG cfg,
			CodeLocation location,
			Expression[] exprs) {
		return new SequenceGetItem(cfg, location, "__getitem__", exprs[0], exprs[1]);
	}

	@Override
	final public void setOriginatingStatement(
			Statement st) {
		this.st = st;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}
