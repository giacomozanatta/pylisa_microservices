package it.unive.pylisa.libraries;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.pylisa.frontend.PyFrontend;
import it.unive.pylisa.program.UnknownModuleUnit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks that the rclpy library specification loads, and that importing
 * {@code rclpy.node} makes every class and callable of the specification
 * available, as importing the real module does through its own imports.
 */
class RclpyLibraryTest {

	private static final Path SPECIFICATION = Path.of("src/main/resources/libraries/rclpy.txt");

	private static final Pattern LIBRARY = Pattern.compile("^library (\\S+):$");

	private static final Pattern CLASS = Pattern.compile("^    class (\\w+) extends \\S+:$");

	private static final Pattern MEMBER = Pattern.compile("^(\\s+)(?:instance )?method (\\w+):");

	private static Program program;

	@BeforeAll
	static void translate() throws Exception {
		program = new PyFrontend("ros-tests/state/libraries/import_rclpy.py", false).toLiSAProgram(true);
	}

	@Test
	void everyDeclaredCallableIsAvailable() throws IOException {
		List<String> missing = new ArrayList<>();
		for (String qualifiedName : declaredCallables())
			if (program.getUnit(qualifiedName) == null)
				missing.add(qualifiedName);
		assertTrue(missing.isEmpty(), "Callables of the specification missing from the program: " + missing);
	}

	@Test
	void theEntityClassesComeWithTheNodeModule() {
		for (String name : List.of("rclpy.node.Node", "rclpy.publisher.Publisher", "rclpy.subscription.Subscription",
				"rclpy.timer.Timer", "rclpy.client.Client", "rclpy.service.Service", "rclpy.context.Context"))
			assertNotNull(program.getUnit(name), name + " is not available");
	}

	@Test
	void aDependencyImportedAgainIsStillTheLibraryModule() {
		Unit publisherModule = program.getUnit("rclpy.publisher");
		assertNotNull(publisherModule);
		assertFalse(publisherModule instanceof UnknownModuleUnit, "rclpy.publisher was turned into an unknown module");
	}

	/**
	 * Parses the qualified names of the callables declared by the
	 * specification: {@code <library>.<function>} for module functions and
	 * {@code <library>.<Class>.<method>} for methods.
	 */
	private static List<String> declaredCallables() throws IOException {
		List<String> names = new ArrayList<>();
		String library = null;
		String currentClass = null;
		for (String line : Files.readAllLines(SPECIFICATION, StandardCharsets.UTF_8)) {
			Matcher matcher;
			if ((matcher = LIBRARY.matcher(line)).find()) {
				library = matcher.group(1);
				currentClass = null;
			} else if ((matcher = CLASS.matcher(line)).find())
				currentClass = matcher.group(1);
			else if ((matcher = MEMBER.matcher(line)).find()) {
				boolean isMethod = matcher.group(1).length() > 4;
				names.add(library + "." + (isMethod ? currentClass + "." : "") + matcher.group(2));
			}
		}
		return names;
	}
}
