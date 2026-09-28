package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that ROS 2 interface types imported by a program are classes whose
 * instances have the fields of their definitions, with their default values,
 * and that an interface package available nowhere does not stop the analysis.
 */
class InterfaceTypesTest {

	private static final String PROGRAM = "ros-tests/state/us0c_interfaces.py";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void messagesAndServicesHaveTheirFields(
			RosConfig config)
			throws Exception {
		RosTestHelper.analyse(PROGRAM, config).assertAllProved();
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void instancesHaveTheirInterfaceType(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse(PROGRAM, config).after("@unknown");
		// class types are named after their definition site: <module>.<Class>@<line>:<column>
		assertTrue(point.object("m").type().startsWith("std_msgs.msg.String@"), point.object("m").type());
		assertTrue(point.object("r").type().startsWith("example_interfaces.srv.AddTwoInts_Request@"),
				point.object("r").type());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void anInterfaceFoundNowhereIsUnknown(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse(PROGRAM, config).after("@unknown");
		assertTrue(point.isReachable());
		assertEquals(Val.top(), point.value("unknown"));
	}
}
