package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.pylisa.checks.AssertionVerdict;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that what the analysis cannot determine stays unknown, never a
 * made-up value, and that the calls rclpy rejects raise its exceptions: invalid
 * names, nodes created before {@code rclpy.init()}, and entities or nodes used
 * after they are destroyed.
 */
class UnknownStateTest {

	private static final String PUBLISHER = "rclpy.publisher.Publisher";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aTopicReadFromInputIsUnknown(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/unknown/topic_from_input.py", config);
		Obj publisher = helper.after("@pub").object("pub");
		assertEquals(PUBLISHER, publisher.type());
		assertEquals(Val.top(), publisher.field("topic_name"));
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@neg"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aTopicChosenByABranchIsEitherName(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/unknown/topic_from_branch.py", config);
		Val topicName = helper.after("@pub").object("pub").field("topic_name");
		if (config == RosConfig.CP)
			assertEquals(Val.top(), topicName);
		else
			assertEquals(Val.oneOf(Set.of("/a", "/b")), topicName);
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@neg"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aSubstitutionIsUnknownAndMayBeRejected(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/unknown/substitution.py", config).after("@pub");
		assertEquals(Val.top(), point.object("pub").field("topic_name"));
		assertTrue(point.errors().contains("rclpy.exceptions.InvalidTopicNameException"), point.errors().toString());
		assertTrue(point.errors().contains("rclpy._rclpy_pybind11.RCLError"), point.errors().toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void invalidNamesRaiseTheirExceptions(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/errors/invalid_names.py", config);
		assertRaised(helper.after("@node"), "rclpy.exceptions.InvalidNodeNameException");
		assertRaised(helper.after("@namespace"), "rclpy.exceptions.InvalidNamespaceException");
		assertRaised(helper.after("@topic"), "rclpy.exceptions.InvalidTopicNameException");
		// no publisher is created on the invalid topic
		for (Obj publisher : helper.after("@topic").objectsOfType(PUBLISHER))
			assertFalse(publisher.field("topic").equals(Val.exact("a//b")), publisher.toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aNodeNeedsAnInitializedContext(
			RosConfig config)
			throws Exception {
		assertRaised(RosTestHelper.analyse("ros-tests/state/errors/node_before_init.py", config).after("@before"),
				"rclpy.exceptions.NotInitializedException");
		Point after = RosTestHelper.analyse("ros-tests/state/errors/node_after_init.py", config).after("@after");
		assertFalse(after.errors().contains("rclpy.exceptions.NotInitializedException"), after.errors().toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void membersOutsideTheModelsAreUnknownCallsWithoutEffect(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/unknown/unmodelled_members.py", config);
		assertEquals(AssertionVerdict.PROVED, helper.verdict("@name"));
		assertEquals(1, helper.after("@name").objectsOfType(PUBLISHER).size());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void runtimeQueriesAreUnknown(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/api_surface.py", config);
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@count"));
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@subscribers"));
		assertEquals(AssertionVerdict.MAY_FAIL, helper.verdict("@ready"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void cancelAndResetChangeTheTimer(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/api_surface.py", config);
		assertEquals(AssertionVerdict.PROVED, helper.verdict("@canceled"));
		assertEquals(AssertionVerdict.PROVED, helper.verdict("@reset"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void destroyingAPublisherLeavesTheOthers(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/api_surface.py", config);
		assertEquals(AssertionVerdict.PROVED, helper.verdict("@destroyed"));
		Point point = helper.after("@destroy");
		assertEquals(Val.exact(true), point.object("pub").field("$destroyed"));
		long alive = point.objectsOfType(PUBLISHER).stream()
				.filter(publisher -> publisher.field("$destroyed").equals(Val.exact(false)))
				.count();
		// the publisher on /parameter_events
		assertEquals(1, alive);
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aDestroyedNodeCannotBeUsed(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/api_surface.py", config);
		assertEquals(Val.exact(true), helper.after("@dn").object("node").field("$destroyed"));
		assertRaised(helper.after("@after"), "rclpy._rclpy_pybind11.InvalidHandle");
	}

	private static void assertRaised(
			Point point,
			String exception) {
		assertTrue(point.errors().contains(exception), point.errors().toString());
	}
}
