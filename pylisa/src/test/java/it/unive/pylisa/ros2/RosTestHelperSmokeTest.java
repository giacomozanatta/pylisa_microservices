package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

/**
 * Checks that {@link RosTestHelper} reads variables, heap objects and their
 * fields correctly on a plain Python program, before it is used to check ROS 2
 * programs.
 */
class RosTestHelperSmokeTest {

	private static final String PROGRAM = "ros-tests/state/smoke.py";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void readsModuleVariables(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(Val.exact("a"), helper.after("@x").value("x"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void readsFieldsOfObjects(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		Point point = helper.after("@f");
		assertEquals(Val.exact("b"), point.value("o.f"));
		assertEquals(Val.exact("b"), point.object("o").field("f"));
		assertTrue(point.object("o").type().endsWith("C@1:0"), point.object("o").type());
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void readsLocalVariablesOfFunctions(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(Val.exact("ns/"), helper.after("@q").value("q"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void distinguishesObjectsByAllocationSite(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		Point point = helper.after("@two");
		assertNotEquals(point.object("o"), point.object("o2"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void joinsTheValuesOfBothBranches(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		Val expected = config == RosConfig.CP ? Val.top() : Val.oneOf(Set.of("a", "b"));
		assertEquals(expected, helper.after("@branch").value("u"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void rejectsLinesWithoutStatementsAndMissingData(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertThrows(AssertionFailedError.class, () -> helper.after(3));
		assertThrows(AssertionFailedError.class, () -> helper.after("@x").value("no_such_variable"));
		assertThrows(AssertionFailedError.class, () -> helper.after("@missing"));
	}
}
