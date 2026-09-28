package it.unive.pylisa.ros2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks the comparison {@code x is None}, which the analysis decides from the
 * value of {@code x}: nothing but {@code None} is {@code None}.
 */
class IdentityTest {

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void identityWithNoneIsDecided(
			RosConfig config)
			throws Exception {
		RosTestHelper.analyse("ros-tests/state/us0c_identity.py", config).assertAllProved();
	}
}
