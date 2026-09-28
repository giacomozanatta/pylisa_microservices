package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that creating a ROS 2 node leaves in the state a node object with the
 * name and namespace given by the program, normalized as rclpy does, together
 * with the entities rclpy creates for every node.
 */
class NodeStateTest {

	private static final String PUBLISHER = "rclpy.publisher.Publisher";

	private static final String SERVICE = "rclpy.service.Service";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aNodeCreatedByCreateNodeHasItsNameAndTheRootNamespace(
			RosConfig config)
			throws Exception {
		RosTestHelper.analyse("ros-tests/state/nodes/create_node.py", config).assertAllProved();
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aNodeCreatedByTheConstructorHasItsNameAndNamespace(
			RosConfig config)
			throws Exception {
		RosTestHelper.analyse("ros-tests/state/nodes/constructor.py", config).assertAllProved();
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aSubclassInitializesItselfAsANode(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/nodes/subclass_namespace.py", config);
		helper.assertAllProved();
		Obj node = helper.after("@super").object("self");
		assertTrue(node.type().contains(".Talker@"), node.type());
		assertEquals(Val.exact("talker"), node.field("$name"));
		assertEquals(Val.exact("/robot1"), node.field("$namespace"));
		assertEquals(Val.exact("/robot1/talker"), node.field("$fqn"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void everyNodePublishesParameterEvents(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/nodes/builtin_entities.py", config).after("@node");
		Set<Obj> publishers = entitiesOf(point, PUBLISHER, point.object("n"));
		assertEquals(1, publishers.size(), publishers.toString());
		Obj events = publishers.iterator().next();
		assertEquals(Val.exact("/parameter_events"), events.field("topic"));
		assertEquals(Val.exact("/parameter_events"), events.field("topic_name"));
		assertEquals(Val.exact(1000), events.field("qos_depth"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void everyNodeOffersTheParameterServicesUnlessDisabled(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/nodes/builtin_entities.py", config).after("@node2");
		Set<Object> names = entitiesOf(point, SERVICE, point.object("n")).stream()
				.map(service -> ((Val.Exact) service.field("service_name")).value())
				.collect(Collectors.toSet());
		assertEquals(Set.of("/n/describe_parameters", "/n/get_parameters", "/n/get_parameter_types",
				"/n/list_parameters", "/n/set_parameters", "/n/set_parameters_atomically"), names);
		assertEquals(Set.of(), entitiesOf(point, SERVICE, point.object("m")));
		assertEquals(1, entitiesOf(point, PUBLISHER, point.object("m")).size());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void addingANodeToAnExecutorRecordsTheExecutor(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/nodes/executors.py", config).after("@add");
		assertEquals(point.object("ex"), point.object("n").ref("executor"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void spinningANodeUsesTheGlobalExecutor(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/nodes/executors.py", config).after("@spin");
		assertEquals("rclpy.executors.SingleThreadedExecutor", point.object("m").ref("executor").type());
	}

	private static Set<Obj> entitiesOf(
			Point point,
			String type,
			Obj node) {
		return point.objectsOfType(type).stream()
				.filter(entity -> entity.refs("$node").contains(node))
				.collect(Collectors.toSet());
	}
}
