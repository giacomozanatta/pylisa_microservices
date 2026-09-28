package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.pylisa.libraries.rclpy.ParameterOverrides;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Analyses every program of the corpus and summarizes, per program, what the
 * analysis finds: the ROS 2 entities, how many topic names are unknown, the
 * rclpy exceptions that may be raised, and the Python constructs the analysis
 * is known to model imprecisely. The summary is compared with a committed
 * baseline, so that any change in what the analysis finds is reviewed; a
 * program that was analysed before and now fails makes the test fail.
 * <p>
 * Running with {@code -Dlisa.cron.update=true} rewrites the baseline.
 * </p>
 */
class CorpusSummaryTest {

	private static final Path SUMMARY = Paths.get("build/ros2-corpus-summary.md");

	private static final Path BASELINE = Paths.get("ros-tests/state/corpus-baseline.md");

	private static final Path OUTCOMES_BEFORE = Paths.get("ros-tests/state/corpus-preport.txt");

	private static final String UPDATE_PROPERTY = "lisa.cron.update";

	private static final String HEADER = "| Program | Outcome | Nodes | Publishers | Subscriptions | Timers | Services"
			+ " | Clients | Unknown topics | rclpy exceptions | Imprecise constructs |";

	/**
	 * The Python constructs the analysis is known to model imprecisely, and
	 * how they are recognized in the source.
	 */
	private static final Map<String, Pattern> CONSTRUCTS = Map.of(
			"f-string", Pattern.compile("\\bf[\"']"),
			"%-format", Pattern.compile("[\"']\\s*%\\s*[(\\w]"),
			"try", Pattern.compile("^\\s*try\\s*:", Pattern.MULTILINE),
			"async", Pattern.compile("\\basync\\s+def\\b"),
			"lambda", Pattern.compile("\\blambda\\b"));

	@AfterEach
	void restoreDefaultOverrides() {
		ParameterOverrides.use(ParameterOverrides.UNKNOWN);
	}

	@Test
	void theCorpusSummaryMatchesTheBaseline() throws IOException {
		Map<String, String> before = outcomesBefore();
		List<String> rows = new ArrayList<>();
		List<String> regressions = new ArrayList<>();
		for (Path program : CorpusRunner.corpusPrograms()) {
			String name = CorpusRunner.CORPUS_ROOT.relativize(program).toString();
			String row = summarize(program, name);
			rows.add(row);
			if ("OK".equals(before.get(name)) && !row.contains("| OK |"))
				regressions.add(name);
		}
		List<String> summary = new ArrayList<>();
		summary.add("# ROS 2 corpus summary (configuration CP, best-effort, no parameter values from outside)");
		summary.add("");
		summary.add(HEADER);
		summary.add("|---|---|---|---|---|---|---|---|---|---|---|");
		summary.addAll(rows);
		Files.createDirectories(SUMMARY.getParent());
		Files.write(SUMMARY, summary, StandardCharsets.UTF_8);
		assertTrue(regressions.isEmpty(), "Programs analysed before now fail: " + regressions);
		if (Boolean.getBoolean(UPDATE_PROPERTY))
			Files.write(BASELINE, summary, StandardCharsets.UTF_8);
		assertEquals(Files.readAllLines(BASELINE, StandardCharsets.UTF_8), summary,
				"The corpus summary differs from " + BASELINE + ": compare with " + SUMMARY
						+ ", and rerun with -D" + UPDATE_PROPERTY + "=true to accept it");
	}

	private static String summarize(
			Path program,
			String name)
			throws IOException {
		String constructs = constructsIn(Files.readString(program, StandardCharsets.UTF_8));
		RosTestHelper helper;
		try {
			// values given from outside the programs are not known: the
			// summary describes the programs as written
			ParameterOverrides.use(ParameterOverrides.NONE);
			helper = RosTestHelper.analyse(program.toString(), RosConfig.CP);
		} catch (Exception | AssertionError | StackOverflowError e) {
			return "| " + name + " | EXCEPTION " + e.getClass().getSimpleName() + " | | | | | | | | | " + constructs
					+ " |";
		}
		Map<String, Map<String, Val>> objects = new HashMap<>();
		Set<String> nodes = new TreeSet<>();
		Set<String> exceptions = new TreeSet<>();
		for (Point point : helper.everyPoint()) {
			point.errors().stream().filter(error -> error.startsWith("rclpy.")).forEach(exceptions::add);
			for (String type : EntityType.NAMES)
				for (Obj entity : point.objectsOfType(type)) {
					Map<String, Val> fields = objects.computeIfAbsent(type + "|" + entity.location(),
							key -> new HashMap<>());
					Optional<Val> topic = entity.fieldIfSet("topic_name");
					topic.ifPresent(value -> fields.merge("topic_name", value, Val::join));
					if (type.equals(EntityType.PUBLISHER))
						entity.refs("$node").forEach(node -> nodes.add(node.location()));
				}
		}
		long unknownTopics = objects.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(EntityType.PUBLISHER + "|")
						|| entry.getKey().startsWith(EntityType.SUBSCRIPTION + "|"))
				.filter(entry -> entry.getValue().get("topic_name") instanceof Val.Top)
				.count();
		return "| " + name + " | OK | " + nodes.size()
				+ " | " + count(objects, EntityType.PUBLISHER)
				+ " | " + count(objects, EntityType.SUBSCRIPTION)
				+ " | " + count(objects, EntityType.TIMER)
				+ " | " + count(objects, EntityType.SERVICE)
				+ " | " + count(objects, EntityType.CLIENT)
				+ " | " + unknownTopics
				+ " | " + exceptions.stream().map(error -> error.substring(error.lastIndexOf('.') + 1))
						.collect(Collectors.joining(", "))
				+ " | " + constructs + " |";
	}

	private static long count(
			Map<String, Map<String, Val>> objects,
			String type) {
		return objects.keySet().stream().filter(key -> key.startsWith(type + "|")).count();
	}

	private static String constructsIn(
			String source) {
		return CONSTRUCTS.entrySet().stream()
				.filter(entry -> entry.getValue().matcher(source).find())
				.map(Map.Entry::getKey)
				.sorted()
				.collect(Collectors.joining(", "));
	}

	private static Map<String, String> outcomesBefore() throws IOException {
		Map<String, String> outcomes = new HashMap<>();
		for (String line : Files.readAllLines(OUTCOMES_BEFORE, StandardCharsets.UTF_8))
			if (!line.startsWith("#") && line.contains("\t")) {
				String[] parts = line.split("\t", 2);
				outcomes.put(parts[0], parts[1].startsWith("OK") ? "OK" : "EXCEPTION");
			}
		return outcomes;
	}

	/**
	 * The entity types the summary counts.
	 */
	private static final class EntityType {

		static final String PUBLISHER = "rclpy.publisher.Publisher";

		static final String SUBSCRIPTION = "rclpy.subscription.Subscription";

		static final String TIMER = "rclpy.timer.Timer";

		static final String SERVICE = "rclpy.service.Service";

		static final String CLIENT = "rclpy.client.Client";

		static final List<String> NAMES = List.of(PUBLISHER, SUBSCRIPTION, TIMER, SERVICE, CLIENT);

		private EntityType() {
		}
	}
}
