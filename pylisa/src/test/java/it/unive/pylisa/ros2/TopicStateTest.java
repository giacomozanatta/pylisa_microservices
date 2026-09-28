package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks that the entities a node creates (publishers, subscriptions, timers,
 * services, clients and guard conditions) are objects in the state with the
 * topic or service name as given and as resolved against the node, the
 * message type, the node that owns them and the callback they run.
 */
class TopicStateTest {

	private static final String PUBLISHER = "rclpy.publisher.Publisher";

	private static final String SUBSCRIPTION = "rclpy.subscription.Subscription";

	private static final String TIMER = "rclpy.timer.Timer";

	private static final String SERVICE = "rclpy.service.Service";

	private static final String CLIENT = "rclpy.client.Client";

	private static final String GUARD_CONDITION = "rclpy.guard_condition.GuardCondition";

	private static final String FUTURE = "rclpy.task.Future";

	@ParameterizedTest
	@ValueSource(strings = { "entities/publisher", "entities/relative_name", "entities/absolute_name",
			"entities/private_name", "entities/keyword_arguments", "entities/two_publishers",
			"entities/subscription", "entities/subscription_lambda", "entities/timers",
			"entities/service_and_client", "entities/resolve_name", "entities/node_in_field" })
	void everyAssertionOnNamesHolds(
			String program)
			throws Exception {
		for (RosConfig config : RosConfig.values())
			RosTestHelper.analyse("ros-tests/state/" + program + ".py", config).assertAllProved();
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aPublisherBelongsToItsNodeAndKnowsItsMessageType(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/entities/publisher.py", config).after("@pub");
		Obj node = point.object("self");
		Obj publisher = node.ref("pub");
		assertEquals(PUBLISHER, publisher.type());
		assertEquals(node, publisher.ref("$node"));
		assertEquals(Val.exact("/chatter"), publisher.field("topic_name"));
		assertEquals(Val.exact(10), publisher.field("qos_depth"));
		assertMessageType(publisher.fieldTypes("msg_type"), "std_msgs.msg.String");
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void anEntityCreatedThroughANodeHeldInAFieldBelongsToThatNode(
			RosConfig config)
			throws Exception {
		Obj app = RosTestHelper.analyse("ros-tests/state/entities/node_in_field.py", config).after("@pub").object("self");
		assertEquals(app.ref("node"), app.ref("pub").ref("$node"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void twoPublishersAreTwoObjects(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/two_publishers.py", config);
		Obj first = helper.after("@p1").object("a");
		Point point = helper.after("@p2");
		Obj second = point.object("b");
		assertNotEquals(first, second);
		assertEquals(Val.exact("/a"), point.object("a").field("topic_name"));
		assertEquals(Val.exact("/b"), second.field("topic_name"));
		assertEquals(Val.exact(5), second.field("qos_depth"));
		// the node publishes on /parameter_events too
		assertEquals(3, point.objectsOfType(PUBLISHER).size(), point.objectsOfType(PUBLISHER).toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aSubscriptionStoresItsCallbackMethod(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/entities/subscription.py", config).after("@sub");
		Obj node = point.object("self");
		Obj subscription = node.ref("sub");
		assertEquals(SUBSCRIPTION, subscription.type());
		assertEquals(node, subscription.ref("$node"));
		assertEquals(Val.exact(10), subscription.field("qos_depth"));
		assertCallback(subscription.fieldTypes("callback"), ".Listener@", ".cb");
		assertMessageType(subscription.fieldTypes("msg_type"), "std_msgs.msg.String");
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aSubscriptionStoresALambdaCallback(
			RosConfig config)
			throws Exception {
		Obj subscription = RosTestHelper.analyse("ros-tests/state/entities/subscription_lambda.py", config).after("@sub")
				.object("sub");
		Set<String> callback = subscription.fieldTypes("callback");
		assertEquals(1, callback.size(), callback.toString());
		assertTrue(callback.iterator().next().contains("lambda"), callback.toString());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aTimerStoresItsPeriodInNanosecondsAndItsCallback(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/entities/timers.py", config).after("@t");
		Obj node = point.object("self");
		Obj timer = node.ref("t");
		assertEquals(TIMER, timer.type());
		assertEquals(node, timer.ref("$node"));
		assertEquals(Val.exact(500_000_000), timer.field("timer_period_ns"));
		assertEquals(Val.exact(false), timer.field("$canceled"));
		assertCallback(timer.fieldTypes("callback"), ".Talker@", ".tick");
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void servicesAndClientsResolveTheirNameUnderTheNamespace(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/service_and_client.py", config);
		Point atService = helper.after("@srv");
		Obj node = atService.object("self");
		Obj service = node.ref("srv");
		assertEquals(SERVICE, service.type());
		assertEquals(node, service.ref("$node"));
		assertCallback(service.fieldTypes("callback"), ".Server@", ".on_add");
		assertMessageType(service.fieldTypes("srv_type"), "example_interfaces.srv.AddTwoInts");

		Obj client = helper.after("@cli").object("self").ref("cli");
		assertEquals(CLIENT, client.type());
		assertEquals(node, client.ref("$node"));
		assertEquals(Val.exact("/robot1/add"), client.field("service_name"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aRequestGivesAFutureWithAnUnknownOutcome(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/entities/service_and_client.py", config).after("@done");
		Obj future = point.object("fut");
		assertEquals(FUTURE, future.type());
		assertEquals(point.object("self").ref("cli"), future.ref("$client"));
		assertEquals(Val.top(), point.value("done"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void aGuardConditionStoresItsCallback(
			RosConfig config)
			throws Exception {
		Point point = RosTestHelper.analyse("ros-tests/state/entities/guard_condition.py", config).after("@g");
		Obj node = point.object("self");
		Obj guard = node.ref("g");
		assertEquals(GUARD_CONDITION, guard.type());
		assertEquals(node, guard.ref("$node"));
		assertCallback(guard.fieldTypes("callback"), ".Waker@", ".wake");
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void edgeCasesFollowRclAndRclpy(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse("ros-tests/state/entities/edge_cases.py", config);
		// ~a expands to the fully qualified name followed by a; a period of
		// -0.1 ns truncates to 0
		helper.assertAllProved();
		// resolving an unknown name may fail, and the C layer reports it as
		// RCLError
		Set<String> resolveErrors = helper.after("@resolve").errors();
		assertTrue(resolveErrors.contains("rclpy._rclpy_pybind11.RCLError"), resolveErrors.toString());
		assertTrue(!resolveErrors.contains("rclpy.exceptions.InvalidTopicNameException"), resolveErrors.toString());
		// None is not a quality of service
		Set<String> qosErrors = helper.after("@qos").errors();
		assertTrue(qosErrors.contains("builtins.TypeError"), qosErrors.toString());
		// an unknown period may be not a number, infinite or out of range
		Set<String> timerErrors = helper.after("@timer").errors();
		assertTrue(timerErrors.containsAll(Set.of("builtins.ValueError", "builtins.OverflowError")), timerErrors.toString());
		// an unknown substitution may be rejected
		Set<String> bracesErrors = helper.after("@braces").errors();
		assertTrue(bracesErrors.contains("rclpy.exceptions.InvalidTopicNameException"), bracesErrors.toString());
		assertEquals(Val.top(), helper.after("@braces").object("braces").field("topic_name"));
	}

	/**
	 * Checks that a callback field holds exactly one method of the expected
	 * class. The receiver of the method is not recorded (known gap
	 * {@code NO_BOUND_METHODS}).
	 */
	private static void assertCallback(
			Set<String> types,
			String owner,
			String method) {
		assertEquals(1, types.size(), types.toString());
		String type = types.iterator().next();
		assertTrue(type.contains(owner) && type.endsWith(method), types.toString());
	}

	/**
	 * Checks that a type field holds exactly the class of an interface
	 * definition, whose type name carries the position of the class after
	 * {@code @}.
	 */
	private static void assertMessageType(
			Set<String> types,
			String qualifiedName) {
		assertEquals(1, types.size(), types.toString());
		assertTrue(types.iterator().next().startsWith(qualifiedName + "@"), types.toString());
	}
}
