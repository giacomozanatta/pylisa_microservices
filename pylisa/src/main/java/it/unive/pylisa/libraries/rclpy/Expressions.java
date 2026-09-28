package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringMatches;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.pylisa.symbolic.PyNoneConstant;

/**
 * Builds the symbolic expressions that the rclpy models evaluate, all placed at
 * the location of the modelled call. Strings are combined with LiSA's string
 * operators, so that every configured value domain can evaluate them.
 */
final class Expressions {

	private final CodeLocation location;

	/**
	 * Builds the factory.
	 *
	 * @param location the location of the modelled call
	 */
	Expressions(
			CodeLocation location) {
		this.location = location;
	}

	/**
	 * Yields the location of the modelled call.
	 *
	 * @return the location
	 */
	CodeLocation location() {
		return location;
	}

	/**
	 * Yields a string constant.
	 *
	 * @param value the string
	 *
	 * @return the constant
	 */
	SymbolicExpression string(
			String value) {
		return new Constant(StringType.INSTANCE, value, location);
	}

	/**
	 * Yields an integer constant.
	 *
	 * @param value the integer
	 *
	 * @return the constant
	 */
	SymbolicExpression integer(
			int value) {
		return new Constant(Int32Type.INSTANCE, value, location);
	}

	/**
	 * Yields a boolean constant.
	 *
	 * @param value the boolean
	 *
	 * @return the constant
	 */
	SymbolicExpression bool(
			boolean value) {
		return new Constant(BoolType.INSTANCE, value, location);
	}

	/**
	 * Yields Python's {@code None}.
	 *
	 * @return the constant
	 */
	SymbolicExpression none() {
		return new PyNoneConstant(location);
	}

	/**
	 * Yields the concatenation of strings, from left to right.
	 *
	 * @param first the first string
	 * @param rest  the other strings
	 *
	 * @return the concatenation
	 */
	SymbolicExpression concat(
			SymbolicExpression first,
			SymbolicExpression... rest) {
		SymbolicExpression result = first;
		for (SymbolicExpression next : rest)
			result = binary(StringType.INSTANCE, StringConcat.INSTANCE, result, next);
		return result;
	}

	/**
	 * Yields the suffix of a string that starts at the given index.
	 *
	 * @param string the string
	 * @param begin  the index of the first character of the suffix
	 *
	 * @return the suffix
	 */
	SymbolicExpression suffix(
			SymbolicExpression string,
			int begin) {
		return binary(StringType.INSTANCE, StringSubstringToEnd.INSTANCE, string, integer(begin));
	}

	/**
	 * Yields the condition {@code left == right}, with Python semantics.
	 *
	 * @param left  the first operand
	 * @param right the second operand
	 *
	 * @return the condition
	 */
	SymbolicExpression equal(
			SymbolicExpression left,
			SymbolicExpression right) {
		return binary(BoolType.INSTANCE, ComparisonEq.INSTANCE, left, right);
	}

	/**
	 * Yields the condition {@code value is None}.
	 *
	 * @param value the value
	 *
	 * @return the condition
	 */
	SymbolicExpression isNone(
			SymbolicExpression value) {
		return equal(value, none());
	}

	/**
	 * Yields the condition that holds when either of two conditions does.
	 *
	 * @param left  the first condition
	 * @param right the second condition
	 *
	 * @return the disjunction
	 */
	SymbolicExpression or(
			SymbolicExpression left,
			SymbolicExpression right) {
		return binary(BoolType.INSTANCE, LogicalOr.INSTANCE, left, right);
	}

	/**
	 * Yields the condition that a string starts with a prefix.
	 *
	 * @param string the string
	 * @param prefix the prefix
	 *
	 * @return the condition
	 */
	SymbolicExpression startsWith(
			SymbolicExpression string,
			String prefix) {
		return binary(BoolType.INSTANCE, StringStartsWith.INSTANCE, string, string(prefix));
	}

	/**
	 * Yields the condition that a string contains another one.
	 *
	 * @param string the string
	 * @param part   the contained string
	 *
	 * @return the condition
	 */
	SymbolicExpression contains(
			SymbolicExpression string,
			String part) {
		return binary(BoolType.INSTANCE, StringContains.INSTANCE, string, string(part));
	}

	/**
	 * Yields the condition that a whole string matches a regular expression.
	 *
	 * @param string  the string
	 * @param pattern the regular expression
	 *
	 * @return the condition
	 */
	SymbolicExpression matches(
			SymbolicExpression string,
			String pattern) {
		return binary(BoolType.INSTANCE, StringMatches.INSTANCE, string, string(pattern));
	}

	private SymbolicExpression binary(
			it.unive.lisa.type.Type type,
			BinaryOperator operator,
			SymbolicExpression left,
			SymbolicExpression right) {
		return new BinaryExpression(type, left, right, operator, location);
	}
}
