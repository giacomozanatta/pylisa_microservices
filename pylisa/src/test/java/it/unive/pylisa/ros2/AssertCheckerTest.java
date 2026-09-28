package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.pylisa.checks.AssertionVerdict;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

/**
 * Checks the translation of {@code assert} statements and the verdicts given
 * to them, on a plain Python program.
 */
class AssertCheckerTest {

	private static final String PROGRAM = "ros-tests/state/assert_basic.py";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void givesEachAssertionItsVerdict(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(AssertionVerdict.PROVED, helper.verdict("@proved"));
		assertEquals(AssertionVerdict.PROVED, helper.verdict("@proved_ne"));
		assertEquals(AssertionVerdict.PROVED, helper.verdict("@proved_message"));
		assertEquals(AssertionVerdict.UNREACHABLE, helper.verdict("@unreachable"));
		assertEquals(AssertionVerdict.FAILS, helper.verdict("@fails"));
		assertEquals(AssertionVerdict.UNREACHABLE, helper.verdict("@after_failure"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aFailingAssertionStopsTheExecutionsThatViolateIt(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertTrue(helper.after("@proved").isReachable());
		assertFalse(helper.after("@fails").isReachable());
		assertTrue(helper.after("@fails").errors().contains("builtins.AssertionError"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void anUndecidedConditionMayFail(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/assert_undecided.py", config);
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@may"));
		assertTrue(helper.after("@may").isReachable());
		assertTrue(helper.after("@may").errors().contains("builtins.AssertionError"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void allProvedFailsOnAnyOtherVerdict(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertThrows(AssertionFailedError.class, helper::assertAllProved);
	}
}
