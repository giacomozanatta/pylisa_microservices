package it.unive.pylisa.testing;

import static org.junit.jupiter.api.Assertions.fail;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.LiSA;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.pylisa.checks.AssertChecker;
import it.unive.pylisa.checks.AssertionVerdict;
import it.unive.pylisa.checks.KnownGap;
import it.unive.pylisa.frontend.ModuleProvider;
import it.unive.pylisa.frontend.PyFrontend;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyses one Python program and gives tests access to the analysis state at
 * any of its program points.
 * <p>
 * Program points are designated either by line number or, more robustly, by a
 * label: a line of the program that ends with the comment {@code # @label} can
 * be referred to as {@code "@label"}. The state at a point is the state right
 * after the statement on that line.
 * </p>
 */
public final class StateTestHelper {

	private final Path program;

	private final AnalysisConfig config;

	private final ResultCollector<?, ?> results;

	private final AssertChecker<?, ?> assertions;

	private final List<String> source;

	private StateTestHelper(
			Path program,
			AnalysisConfig config,
			ResultCollector<?, ?> results,
			AssertChecker<?, ?> assertions,
			List<String> source) {
		this.program = program;
		this.config = config;
		this.results = results;
		this.assertions = assertions;
		this.source = source;
	}

	/**
	 * Analyses a program.
	 *
	 * @param program   the path of the Python entry file, relative to the
	 *                      project directory
	 * @param config    the analysis configuration
	 * @param providers the providers of modules the program imports that are
	 *                      neither in the program nor in pylisa's library
	 *                      specifications
	 *
	 * @return the helper giving access to the results
	 *
	 * @throws IOException            if the program cannot be read
	 * @throws AnalysisSetupException if the program cannot be translated
	 * @throws AnalysisException      if the analysis fails
	 */
	public static StateTestHelper analyse(
			String program,
			AnalysisConfig config,
			ModuleProvider... providers)
			throws IOException,
			AnalysisSetupException,
			AnalysisException {
		Path path = Paths.get(program);
		PyFrontend frontend = new PyFrontend(program, false);
		for (ModuleProvider provider : providers)
			frontend.addModuleProvider(provider);
		Program lisaProgram = frontend.toLiSAProgram(true);
		LiSAConfiguration conf = config.configuration(stem(path) + "/" + config.name());
		ResultCollector<?, ?> collector = new ResultCollector<>();
		AssertChecker<?, ?> checker = new AssertChecker<>();
		conf.semanticChecks.add(collector);
		conf.semanticChecks.add(checker);
		new LiSA(conf).run(lisaProgram);
		return new StateTestHelper(path, config, collector, checker, Files.readAllLines(path, StandardCharsets.UTF_8));
	}

	/**
	 * Yields the configuration the program was analysed with.
	 *
	 * @return the configuration
	 */
	public AnalysisConfig config() {
		return config;
	}

	/**
	 * Yields the known ways in which the analysis may miss executions of the
	 * program: the results of this helper hold only for the executions the
	 * analysis models.
	 *
	 * @return the known gaps
	 */
	public Set<KnownGap> knownGaps() {
		return EnumSet.allOf(KnownGap.class);
	}

	/**
	 * Yields the state after the statement on the line marked with the given
	 * label.
	 *
	 * @param label the label, starting with {@code @}
	 *
	 * @return the state
	 */
	public Point after(
			String label) {
		return after(lineOf(label));
	}

	/**
	 * Yields the state after the first statement on the given line.
	 *
	 * @param line the 1-based line number
	 *
	 * @return the state
	 */
	public Point after(
			int line) {
		return after(line, 0);
	}

	/**
	 * Yields the state after one of the statements on the given line, ordered
	 * by column.
	 *
	 * @param line       the 1-based line number
	 * @param occurrence the 0-based index of the statement among those on the
	 *                       line
	 *
	 * @return the state
	 */
	public Point after(
			int line,
			int occurrence) {
		List<Statement> onLine = results.statements().stream()
				.filter(statement -> isAt(statement, line))
				.sorted(Comparator.comparingInt(statement -> ((SourceCodeLocation) statement.getLocation()).getCol()))
				.collect(Collectors.toList());
		if (occurrence >= onLine.size())
			return fail("Line " + line + " of " + program + " has " + onLine.size() + " statements, no statement #"
					+ occurrence);
		Statement statement = onLine.get(occurrence);
		return results.after(statement, config.reader())
				.map(Point::of)
				.orElseGet(() -> Point.unanalysed("after " + statement + " at " + statement.getLocation()));
	}

	/**
	 * Yields the state after every analysed statement, in every file of the
	 * program, joined over contexts. Statements that no execution reaches are
	 * left out.
	 *
	 * @return the states
	 */
	public List<Point> everyPoint() {
		return results.statements().stream()
				.map(statement -> results.after(statement, config.reader()).map(Point::of))
				.flatMap(Optional::stream)
				.filter(Point::isReachable)
				.collect(Collectors.toList());
	}

	/**
	 * Yields the verdict of every {@code assert} statement of the analysed
	 * program file, by line.
	 *
	 * @return the verdicts, sorted by line
	 */
	public Map<Integer, AssertionVerdict> asserts() {
		Map<Integer, AssertionVerdict> byLine = new TreeMap<>();
		for (Map.Entry<CodeLocation, AssertionVerdict> entry : assertions.getVerdicts().entrySet())
			if (entry.getKey() instanceof SourceCodeLocation location && isInProgram(location))
				byLine.merge(location.getLine(), entry.getValue(), (first, second) -> first == second
						? first
						: AssertionVerdict.MAY_FAIL);
		return byLine;
	}

	/**
	 * Yields the verdict of the {@code assert} statement on the line marked
	 * with the given label.
	 *
	 * @param label the label, starting with {@code @}
	 *
	 * @return the verdict
	 */
	public AssertionVerdict verdict(
			String label) {
		int line = lineOf(label);
		AssertionVerdict verdict = asserts().get(line);
		if (verdict == null)
			return fail("Line " + line + " of " + program + " holds no analysed assertion");
		return verdict;
	}

	/**
	 * Fails unless every {@code assert} statement of the analysed program file
	 * is proved. An assertion that no execution reaches is not proved.
	 */
	public void assertAllProved() {
		Map<Integer, AssertionVerdict> verdicts = asserts();
		if (verdicts.isEmpty())
			fail(program + " holds no analysed assertion");
		Map<Integer, AssertionVerdict> notProved = new TreeMap<>(verdicts);
		notProved.values().removeIf(AssertionVerdict.PROVED::equals);
		if (!notProved.isEmpty())
			fail("Assertions of " + program + " not proved (" + config.label() + "), by line: " + notProved);
	}

	private boolean isAt(
			Statement statement,
			int line) {
		return statement.getLocation() instanceof SourceCodeLocation location
				&& location.getLine() == line
				&& isInProgram(location);
	}

	private boolean isInProgram(
			SourceCodeLocation location) {
		return Paths.get(location.getSourceFile()).normalize().equals(program.normalize());
	}

	private int lineOf(
			String label) {
		if (!label.startsWith("@"))
			throw new IllegalArgumentException("A label starts with @, got " + label);
		Pattern marker = Pattern.compile("#\\s*" + Pattern.quote(label) + "\\s*$");
		int found = -1;
		for (int i = 0; i < source.size(); i++)
			if (marker.matcher(source.get(i)).find()) {
				if (found != -1)
					return fail("Label " + label + " marks more than one line of " + program);
				found = i + 1;
			}
		if (found == -1)
			return fail("No line of " + program + " is marked with " + label);
		return found;
	}

	private static String stem(
			Path program) {
		String name = program.getFileName().toString();
		int dot = name.lastIndexOf('.');
		return dot == -1 ? name : name.substring(0, dot);
	}
}
