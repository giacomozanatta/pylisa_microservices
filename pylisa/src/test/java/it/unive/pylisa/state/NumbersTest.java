package it.unive.pylisa.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.pylisa.checks.AssertionVerdict;
import it.unive.pylisa.testing.AnalysisConfig;
import it.unive.pylisa.testing.StateTestHelper;
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
	@EnumSource(AnalysisConfig.class)
	void arithmeticFollowsPython(
			AnalysisConfig config)
			throws Exception {
		StateTestHelper helper = StateTestHelper.analyse("src/test/resources/programs/python/arithmetic.py", config);
		Map<Integer, AssertionVerdict> notProved = helper.asserts().entrySet().stream()
				.filter(entry -> entry.getValue() != AssertionVerdict.PROVED)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// the comparisons of a chain after the first are not checked, so
		// neither the chain nor its negation is proved
		assertEquals(Map.of(18, AssertionVerdict.MAY_FAIL, 19, AssertionVerdict.MAY_FAIL), notProved);
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@chain"));
	}
}
