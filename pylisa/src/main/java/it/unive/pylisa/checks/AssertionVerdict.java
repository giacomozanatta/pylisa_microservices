package it.unive.pylisa.checks;

import it.unive.lisa.lattices.Satisfiability;

/**
 * What the analysis established about an {@code assert} statement, over every
 * execution that reaches it.
 */
public enum AssertionVerdict {

	/**
	 * The condition holds in every execution that reaches the assertion.
	 */
	PROVED,

	/**
	 * The condition is false in every execution that reaches the assertion.
	 */
	FAILS,

	/**
	 * The analysis could not decide the condition: it may hold in some
	 * executions and fail in others, or be too imprecise to tell.
	 */
	MAY_FAIL,

	/**
	 * No execution reaches the assertion, so nothing was checked.
	 */
	UNREACHABLE;

	/**
	 * Yields the verdict for one analysis context from the satisfiability of
	 * the condition in that context.
	 *
	 * @param satisfiability the satisfiability of the condition
	 *
	 * @return the verdict
	 */
	static AssertionVerdict of(
			Satisfiability satisfiability) {
		switch (satisfiability) {
		case SATISFIED:
			return PROVED;
		case NOT_SATISFIED:
			return FAILS;
		case BOTTOM:
			return UNREACHABLE;
		default:
			return MAY_FAIL;
		}
	}

	/**
	 * Combines the verdicts of two sets of executions reaching the same
	 * assertion, such as two analysis contexts: the assertion is proved (or
	 * fails) only if it is proved (or fails) in both, and executions that do
	 * not reach it do not count.
	 *
	 * @param other the verdict of the other executions
	 *
	 * @return the combined verdict
	 */
	AssertionVerdict combine(
			AssertionVerdict other) {
		if (this == UNREACHABLE)
			return other;
		if (other == UNREACHABLE || other == this)
			return this;
		return MAY_FAIL;
	}
}
