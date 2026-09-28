package it.unive.pylisa.ros2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that string literals with a prefix never evaluate to their own source
 * text: formatted strings and bytes are unknown values, while raw and unicode
 * strings evaluate to their contents.
 */
class StringLiteralTest {

	private static final String PROGRAM = "ros-tests/state/python/string_literals.py";

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void formattedStringsAreUnknown(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(Val.top(), helper.after("@formatted").value("t"));
		assertEquals(Val.top(), helper.after("@formatted_upper").value("u"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void rawAndUnicodeStringsKeepTheirContents(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(Val.exact("/a\\b"), helper.after("@raw").value("raw"));
		assertEquals(Val.exact("/plain"), helper.after("@unicode_prefix").value("plain"));
	}

	@ParameterizedTest
	@EnumSource(RosConfig.class)
	void bytesAreUnknown(
			RosConfig config)
			throws Exception {
		RosTestHelper helper = RosTestHelper.analyse(PROGRAM, config);
		assertEquals(Val.top(), helper.after("@bytes").value("data"));
	}
}
