package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks the state of a talker and a listener taken unchanged from a ROS 2
 * example: the nodes, their names, and the publisher and subscription on
 * {@code /chatter}. The programs carry no labels, so the statements are found
 * by their text. What callbacks do is not checked: they are not run.
 */
class TalkerListenerTest {

	private static final String TALKER = "ros-tests/talker_listener/talker.py";

	private static final String LISTENER = "ros-tests/talker_listener/listener.py";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void theTalkerPublishesOnChatter(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse(TALKER, config).after(lineOf(TALKER, "self.pub = self.create_publisher"));
		Obj node = point.object("self");
		assertEquals(Val.exact("talker"), node.field("$name"));
		Set<Obj> publishers = entitiesOf(point, "rclpy.publisher.Publisher", node);
		Set<Object> topics = publishers.stream()
				.map(publisher -> ((Val.Exact) publisher.field("topic_name")).value())
				.collect(Collectors.toSet());
		assertEquals(Set.of("/parameter_events", "/chatter"), topics);
		assertEquals(Val.exact("/chatter"), node.ref("pub").field("topic_name"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void theListenerSubscribesToChatterWithItsMethod(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse(LISTENER, config)
				.after(lineOf(LISTENER, "self.sub = self.create_subscription"));
		Obj node = point.object("self");
		assertEquals(Val.exact("listener"), node.field("$name"));
		Obj subscription = node.ref("sub");
		assertEquals(Val.exact("/chatter"), subscription.field("topic_name"));
		assertEquals(node, subscription.ref("$node"));
		Set<String> callback = subscription.fieldTypes("callback");
		assertEquals(1, callback.size(), callback.toString());
		assertTrue(callback.iterator().next().contains(".Listener@") && callback.iterator().next().endsWith(".cb"),
				callback.toString());
	}

	private static Set<Obj> entitiesOf(
			Point point,
			String type,
			Obj node) {
		return point.objectsOfType(type).stream()
				.filter(entity -> entity.refs("$node").contains(node))
				.collect(Collectors.toSet());
	}

	/**
	 * Yields the number of the only line of a program that contains a text.
	 */
	private static int lineOf(
			String program,
			String text)
			throws IOException {
		List<String> lines = Files.readAllLines(Path.of(program));
		int found = -1;
		for (int i = 0; i < lines.size(); i++)
			if (lines.get(i).contains(text)) {
				if (found != -1)
					fail("More than one line of " + program + " contains " + text);
				found = i + 1;
			}
		if (found == -1)
			fail("No line of " + program + " contains " + text);
		return found;
	}
}
