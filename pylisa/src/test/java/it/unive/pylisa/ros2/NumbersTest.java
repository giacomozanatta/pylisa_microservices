package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.pylisa.checks.AssertionVerdict;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that arithmetic and comparisons on numbers give Python's results:
 * operators group to the left, booleans are numbers, floats are doubles, and
 * a chained comparison claims nothing it has not checked.
 */
class NumbersTest {

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void arithmeticFollowsPython(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/python/arithmetic.py", config);
		Map<Integer, AssertionVerdict> notProved = helper.asserts().entrySet().stream()
				.filter(entry -> entry.getValue() != AssertionVerdict.PROVED)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// the comparisons of a chain after the first are not checked, so
		// neither the chain nor its negation is proved
		assertEquals(Map.of(18, AssertionVerdict.MAY_FAIL, 19, AssertionVerdict.MAY_FAIL), notProved);
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@chain"));
	}
}
