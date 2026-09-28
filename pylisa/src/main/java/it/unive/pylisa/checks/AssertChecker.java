package it.unive.pylisa.checks;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.pylisa.cfg.statement.PyAssert;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A semantic check that decides, for every {@code assert} statement of the
 * analysed program, whether its condition holds in the executions that reach
 * it.
 * <p>
 * The condition is evaluated in the state right before the assertion, in each
 * analysis context of the enclosing function, and the per-context verdicts are
 * combined (see {@link AssertionVerdict#combine}). Assertions that are not
 * proved are also reported as warnings of the analysis.
 * </p>
 * <p>
 * The verdicts are as reliable as the analysis that computed the states: when
 * calls to unknown code are assumed to have no effect, a proved assertion is
 * proved only under that assumption.
 * </p>
 *
 * @param <A> the kind of abstract state
 * @param <D> the kind of abstract domain
 */
public class AssertChecker<A extends AbstractLattice<A>, D extends AbstractDomain<A>> implements SemanticCheck<A, D> {

	private final Map<CodeLocation, AssertionVerdict> verdicts = new LinkedHashMap<>();

	/**
	 * Yields the verdict of every assertion checked so far, by the location of
	 * the assertion.
	 *
	 * @return the verdicts, in the order the assertions were checked
	 */
	public Map<CodeLocation, AssertionVerdict> getVerdicts() {
		return Collections.unmodifiableMap(verdicts);
	}

	@Override
	public boolean visit(
			SemanticTool<A, D> tool,
			CFG graph,
			Statement node) {
		if (!(node instanceof PyAssert assertion))
			return true;
		AssertionVerdict verdict = AssertionVerdict.UNREACHABLE;
		for (AnalyzedCFG<A> result : tool.getResultOf(graph))
			verdict = verdict.combine(verdictIn(tool.getAnalysis(), result, assertion));
		verdicts.merge(assertion.getLocation(), verdict, AssertionVerdict::combine);
		if (verdict == AssertionVerdict.FAILS)
			tool.warnOn(assertion, "The assertion fails in every execution that reaches it");
		else if (verdict == AssertionVerdict.MAY_FAIL)
			tool.warnOn(assertion, "The assertion may fail");
		return true;
	}

	private AssertionVerdict verdictIn(
			Analysis<A, D> analysis,
			AnalyzedCFG<A> result,
			PyAssert assertion) {
		try {
			AnalysisState<A> beforeAssertion = result.getAnalysisStateAfter(assertion.getCondition());
			if (beforeAssertion.getExecution().isBottom() || beforeAssertion.getExecutionState().isBottom())
				return AssertionVerdict.UNREACHABLE;
			AssertionVerdict verdict = AssertionVerdict.UNREACHABLE;
			for (SymbolicExpression condition : beforeAssertion.getExecutionExpressions()) {
				Satisfiability satisfiability = analysis.satisfies(beforeAssertion, condition, assertion);
				verdict = verdict.combine(AssertionVerdict.of(satisfiability));
			}
			return verdict;
		} catch (SemanticException e) {
			throw new IllegalStateException("Cannot evaluate the condition of " + assertion + " at "
					+ assertion.getLocation(), e);
		}
	}
}
