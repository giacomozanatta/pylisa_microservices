package it.unive.pylisa.ros2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that the arguments of a call are bound to the parameters of the
 * callee as Python binds them, in particular when the call relies on default
 * values and passes no keyword argument.
 */
class BindingTest {

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void argumentsAreBoundAsInPython(
			RosConfig config)
			throws Exception {
		RosTestHelper.analyse("ros-tests/state/us0b_binding.py", config).assertAllProved();
	}
}
