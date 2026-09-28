package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks what publishing a message leaves in the state: the publisher that
 * sends it and the message itself can be read at the call, and a message that
 * is not an instance of the publisher's message type makes the call raise
 * {@code TypeError}, as rclpy's {@code Publisher.publish} does.
 */
class PublishStateTest {

	private static final String TYPE_ERROR = "builtins.TypeError";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void thePublisherAndTheMessageAreVisibleAtThePublish(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/entities/publish.py", config).after("@publish");
		Obj publisher = point.object("self").ref("pub");
		assertEquals("rclpy.publisher.Publisher", publisher.type());
		assertEquals(Val.exact("/chatter"), publisher.field("topic_name"));
		assertEquals(Val.exact("hi"), point.object("msg").field("data"));
		assertFalse(point.errors().contains(TYPE_ERROR), point.errors().toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aMessageOfAnotherTypeIsRejected(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/publish_wrong_type.py", config);
		Set<String> afterInt = helper.after("@int").errors();
		assertTrue(afterInt.contains(TYPE_ERROR), afterInt.toString());
		assertTrue(helper.after("@right").isReachable());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aMessageOfAnotherInterfaceIsRejected(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/publish_wrong_type.py", config);
		Set<String> afterOther = helper.after("@other").errors();
		assertTrue(afterOther.contains(TYPE_ERROR), afterOther.toString());
	}
}
