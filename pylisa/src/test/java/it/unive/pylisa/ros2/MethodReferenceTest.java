package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks what a method reference such as {@code self.cb} evaluates to when it
 * is used as a value rather than called. The value identifies the method of
 * the class; the object it is read from is not kept with it, so calling the
 * value later does not pass the receiver (a known gap of the analysis).
 */
class MethodReferenceTest {

	private static final String PROGRAM = "ros-tests/state/python/method_reference.py";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aMethodPassedAsArgumentIdentifiesTheMethod(
			RosConfig config)
			throws Exception {
		Set<String> types = RosTestHelper.analyse(PROGRAM, config).after("@x").types("x");
		assertEquals(1, types.size(), types.toString());
		assertTrue(types.iterator().next().endsWith(".cb"), types.toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aMethodStoredInAVariableIdentifiesTheMethod(
			RosConfig config)
			throws Exception {
		Set<String> types = RosTestHelper.analyse(PROGRAM, config).after("@y").types("y");
		assertEquals(1, types.size(), types.toString());
		assertTrue(types.iterator().next().endsWith(".cb"), types.toString());
	}
}
