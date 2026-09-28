package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.pylisa.libraries.rclpy.ParameterOverrides;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks the parameters of a node: a declared parameter is read back with the
 * value the program gives it, unless a value may be given from outside the
 * program, which is what the client of the analysis says through
 * {@link ParameterOverrides}; by default any parameter may be overridden.
 */
class ParameterStateTest {

	private static final String PROGRAM = "ros-tests/state/us2_param_topic.py";

	private static final String NODES = "ros-tests/state/us2_param_node.py";

	@AfterEach
	void restoreDefaultOverrides() {
		ParameterOverrides.use(ParameterOverrides.UNKNOWN);
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void byDefaultAParameterMayBeOverriddenFromOutside(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(Val.top(), helper.after("@pub").object("self").ref("pub").field("topic_name"));
		// an override of another type makes the declaration fail
		Set<String> errors = helper.after("@value").errors();
		assertTrue(errors.contains("rclpy.exceptions.InvalidParameterTypeException"), errors.toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void withoutOverridesAParameterHasItsDeclaredValue(
			RosConfig config)
			throws Exception {
		ParameterOverrides.use(ParameterOverrides.NONE);
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		helper.assertAllProved();
		assertEquals(Val.exact("/chatter"), helper.after("@pub").object("self").ref("pub").field("topic_name"));
		assertTrue(!helper.after("@value").errors().contains("rclpy.exceptions.InvalidParameterTypeException"));
		Set<String> errors = helper.after("@undeclared").errors();
		assertTrue(errors.contains("rclpy.exceptions.ParameterNotDeclaredException"), errors.toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aKnownOverrideReplacesTheDeclaredValue(
			RosConfig config)
			throws Exception {
		ParameterOverrides.use(ParameterOverrides.of(List.of(new ParameterOverrides.Known("/n", "topic", "other")),
				false));
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(Val.exact("/other"), helper.after("@pub").object("self").ref("pub").field("topic_name"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void overridesGivenByTheProgramAndUntrackedChangesMakeValuesUnknown(
			RosConfig config)
			throws Exception {
		ParameterOverrides.use(ParameterOverrides.NONE);
		RosTestHelper helper = RosTestHelper.analyse(NODES, config);
		// the program passes a list of overrides, which is not inspected
		assertEquals(Val.top(), helper.after("@given").value("t"));
		// undeclared parameters may be read when the node allows them
		helper.assertAllProved();
		// setting several parameters at once is not tracked
		assertEquals(Val.top(), helper.after("@changed").value("r"));
		Set<String> errors = helper.after("@again").errors();
		assertTrue(errors.contains("rclpy.exceptions.ParameterAlreadyDeclaredException"), errors.toString());
	}
}
