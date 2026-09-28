package it.unive.pylisa.ros2;

import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.Program;
import it.unive.pylisa.frontend.PyFrontend;
import it.unive.pylisa.testutil.LiSAConfigs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Analyses every Python program of the ROS 2 test corpus with the default
 * configuration and records, for each program, whether the analysis runs to
 * completion.
 * <p>
 * The recorded outcomes are a regression baseline: a program that used to be
 * analysed without errors must keep being analysed without errors when the
 * frontend or its library models change. The runner is disabled unless the
 * system property {@value #OUTPUT_PROPERTY} names the file to write, so that it
 * never runs as part of the regular test suite.
 * </p>
 */
class CorpusRunner {

	/**
	 * The system property holding the path of the file the outcomes are written
	 * to.
	 */
	static final String OUTPUT_PROPERTY = "lisa.ros2.corpus.out";

	/**
	 * The root directory of the corpus, relative to the project directory.
	 */
	static final Path CORPUS_ROOT = Paths.get("ros-tests");

	/**
	 * Directories of the corpus root that hold purpose-written programs or
	 * ground truth rather than corpus programs.
	 */
	private static final Set<String> EXCLUDED_DIRECTORIES = Set.of("state", "talker_listener");

	private static final Logger LOG = LogManager.getLogger(CorpusRunner.class);

	/**
	 * Analyses the whole corpus and writes one line per program, in the form
	 * {@code <path>\t<outcome>}, sorted by path.
	 *
	 * @throws IOException if the corpus cannot be listed or the output cannot
	 *                         be written
	 */
	@Test
	@EnabledIfSystemProperty(named = OUTPUT_PROPERTY, matches = ".+")
	void recordOutcomes() throws IOException {
		List<String> lines = new ArrayList<>();
		for (Path program : corpusPrograms())
			lines.add(CORPUS_ROOT.relativize(program) + "\t" + outcomeOf(program));
		Files.write(Paths.get(System.getProperty(OUTPUT_PROPERTY)), lines, StandardCharsets.UTF_8);
	}

	/**
	 * Yields the Python files of the corpus, sorted by path.
	 *
	 * @return the corpus programs
	 *
	 * @throws IOException if the corpus cannot be listed
	 */
	static List<Path> corpusPrograms() throws IOException {
		try (Stream<Path> files = Files.walk(CORPUS_ROOT)) {
			return files
					.filter(p -> p.toString().endsWith(".py"))
					.filter(p -> !EXCLUDED_DIRECTORIES.contains(CORPUS_ROOT.relativize(p).getName(0).toString()))
					.sorted()
					.collect(Collectors.toList());
		}
	}

	/**
	 * Parses and analyses one program.
	 *
	 * @param program the program to analyse
	 *
	 * @return {@code OK} if the analysis completed, or {@code EXCEPTION}
	 *             followed by the class of the throwable that stopped it
	 */
	static String outcomeOf(
			Path program) {
		try {
			Program lisaProgram = new PyFrontend(program.toString(), false).toLiSAProgram(true);
			LiSAConfiguration conf = LiSAConfigs.getDefaultConf("ros2-corpus/" + program.getFileName());
			conf.outputs.clear();
			new LiSA(conf).run(lisaProgram);
			return "OK";
		} catch (Exception | StackOverflowError e) {
			// the outcome is the result being recorded: the failure is logged
			// and reported in the output, not swallowed
			LOG.warn("Analysis of {} failed", program, e);
			return "EXCEPTION " + e.getClass().getName();
		}
	}
}
