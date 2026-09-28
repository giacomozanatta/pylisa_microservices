package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checks that the rclpy models keep their promises to the readers of the
 * analysis state: every field an rclpy object is documented to hold is
 * checked by some test, and every modelled callable is bound to its model in
 * the library specification and exercised by some test program.
 * <p>
 * The lists below are the interface between the models and their readers;
 * changing a name here is a breaking change for them.
 * </p>
 */
class ContractCoverageTest {

	private static final Path TESTS = Paths.get("src/test/java/it/unive/pylisa/ros2");

	private static final Path PROGRAMS = Paths.get("ros-tests");

	private static final Path LIBRARY = Paths.get("src/main/resources/libraries/rclpy.txt");

	/**
	 * The fields of the objects the rclpy models create, by class.
	 */
	private static final Map<String, List<String>> FIELDS = Map.of(
			"rclpy.node.Node", List.of("$name", "$namespace", "$fqn", "$destroyed", "executor"),
			"rclpy.publisher.Publisher", List.of("msg_type", "topic", "topic_name", "qos_profile", "qos_depth",
					"$node", "$destroyed"),
			"rclpy.subscription.Subscription", List.of("msg_type", "topic", "topic_name", "qos_depth", "callback",
					"raw", "$node"),
			"rclpy.timer.Timer", List.of("timer_period_ns", "callback", "$canceled", "$node"),
			"rclpy.service.Service", List.of("srv_type", "srv_name", "service_name", "callback", "$node"),
			"rclpy.client.Client", List.of("srv_name", "service_name", "$node"),
			"rclpy.guard_condition.GuardCondition", List.of("callback", "$node"),
			"rclpy.task.Future", List.of("$client"),
			"rclpy.parameter.Parameter", List.of("name", "value"));

	/**
	 * The modelled callables and their models; every other callable of the
	 * library specification has an unknown result and no effect.
	 */
	private static final Map<String, String> MODELS = Map.ofEntries(
			Map.entry("rclpy.init", "RclpyInit"),
			Map.entry("rclpy.shutdown", "RclpyShutdown"),
			Map.entry("rclpy.ok", "RclpyOk"),
			Map.entry("rclpy.create_node", "CreateNode"),
			Map.entry("rclpy.spin", "Spin"),
			Map.entry("rclpy.spin_once", "SpinOnce"),
			Map.entry("Node.__init__", "NodeInit"),
			Map.entry("Node.get_name", "GetName"),
			Map.entry("Node.get_namespace", "GetNamespace"),
			Map.entry("Node.get_fully_qualified_name", "GetFullyQualifiedName"),
			Map.entry("Node.create_publisher", "CreatePublisher"),
			Map.entry("Node.create_subscription", "CreateSubscription"),
			Map.entry("Node.create_timer", "CreateTimer"),
			Map.entry("Node.create_service", "CreateService"),
			Map.entry("Node.create_client", "CreateClient"),
			Map.entry("Node.create_guard_condition", "CreateGuardCondition"),
			Map.entry("Node.resolve_topic_name", "ResolveTopicName"),
			Map.entry("Node.resolve_service_name", "ResolveServiceName"),
			Map.entry("Node.declare_parameter", "DeclareParameter"),
			Map.entry("Node.get_parameter", "GetParameter"),
			Map.entry("Node.get_parameter_or", "GetParameterOr"),
			Map.entry("Node.has_parameter", "HasParameter"),
			Map.entry("Node.set_parameters", "ChangeParameters"),
			Map.entry("Node.add_on_set_parameters_callback", "AddSetParametersCallback"),
			Map.entry("Node.destroy_publisher", "DestroyEntity"),
			Map.entry("Node.destroy_node", "DestroyNode"),
			Map.entry("Executor.add_node", "AddNode"),
			Map.entry("Publisher.publish", "Publish"),
			Map.entry("Client.call_async", "CallAsync"),
			Map.entry("Timer.cancel", "CancelTimer"),
			Map.entry("Timer.reset", "ResetTimer"),
			Map.entry("Timer.is_canceled", "IsCanceled"),
			Map.entry("Parameter.__init__", "ParameterInit"));

	@Test
	void everyFieldIsCheckedBySomeTest() throws IOException {
		String tests = contentsOf(TESTS, ".java");
		String programs = contentsOf(PROGRAMS, ".py");
		List<String> unchecked = new ArrayList<>();
		FIELDS.forEach((type, fields) -> {
			for (String field : fields)
				if (!tests.contains("\"" + field + "\"") && !programs.contains("." + field))
					unchecked.add(type + "." + field);
		});
		assertEquals(List.of(), unchecked);
	}

	@Test
	void everyModelledCallableIsBoundToItsModel() throws IOException {
		Map<String, String> bindings = bindings();
		List<String> wrong = MODELS.entrySet().stream()
				.filter(entry -> !entry.getValue().equals(bindings.get(entry.getKey())))
				.map(entry -> entry.getKey() + " -> " + bindings.get(entry.getKey()))
				.sorted()
				.collect(Collectors.toList());
		assertEquals(List.of(), wrong);
	}

	@Test
	void everyModelledCallableIsCalledBySomeTestProgram() throws IOException {
		String programs = contentsOf(PROGRAMS.resolve("state"), ".py")
				+ contentsOf(PROGRAMS.resolve("talker_listener"), ".py");
		List<String> uncalled = MODELS.keySet().stream()
				.filter(callable -> !isCalled(programs, callable))
				.sorted()
				.collect(Collectors.toList());
		assertEquals(List.of(), uncalled);
	}

	private static boolean isCalled(
			String programs,
			String callable) {
		String method = callable.substring(callable.lastIndexOf('.') + 1);
		if (method.equals("__init__")) {
			// constructors are called through the class name or super()
			String owner = callable.substring(0, callable.indexOf('.'));
			return programs.contains(owner + "(") || programs.contains("super().__init__(");
		}
		return Pattern.compile("[.\\s]" + Pattern.quote(method) + "\\(").matcher(programs).find();
	}

	/**
	 * Yields the model bound to every callable of the library specification,
	 * by {@code Class.method} or {@code module.function}.
	 */
	private static Map<String, String> bindings() throws IOException {
		Pattern library = Pattern.compile("^library (\\S+):");
		Pattern type = Pattern.compile("^\\s+class (\\w+)");
		Pattern member = Pattern.compile("^\\s+(?:instance )?method (\\w+): it\\.unive\\.pylisa\\.libraries\\.rclpy\\.(\\w+)");
		String module = null;
		String owner = null;
		Map<String, String> result = new java.util.HashMap<>();
		for (String line : Files.readAllLines(LIBRARY, StandardCharsets.UTF_8)) {
			Matcher matcher;
			if ((matcher = library.matcher(line)).find()) {
				module = matcher.group(1);
				owner = null;
			} else if ((matcher = type.matcher(line)).find())
				owner = matcher.group(1);
			else if ((matcher = member.matcher(line)).find())
				result.put((owner != null && line.startsWith("        ") ? owner : module) + "." + matcher.group(1),
						matcher.group(2));
		}
		return result;
	}

	private static String contentsOf(
			Path root,
			String extension)
			throws IOException {
		try (Stream<Path> files = Files.walk(root)) {
			StringBuilder contents = new StringBuilder();
			for (Path file : files.filter(p -> p.toString().endsWith(extension)).collect(Collectors.toList()))
				contents.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
			return contents.toString();
		}
	}
}
