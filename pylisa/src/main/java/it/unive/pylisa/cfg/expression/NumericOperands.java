package it.unive.pylisa.cfg.expression;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

/**
 * The arithmetic of Python numbers between two operands. Python's numbers
 * include booleans, which count as the integers {@code 0} and {@code 1}: the
 * operation is computed where each operand may be a number or a boolean.
 * Operands of other types (lists, tuples, objects defining the special
 * methods) are not modelled, and give an unknown result.
 */
final class NumericOperands {

	private NumericOperands() {
	}

	/**
	 * Computes the semantics of an arithmetic operation.
	 *
	 * @param <A>             the kind of abstract state
	 * @param <D>             the kind of abstract domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state before the operation
	 * @param left            the first operand
	 * @param right           the second operand
	 * @param operator        the operator
	 * @param statement       the statement performing the operation
	 *
	 * @return the state after the operation
	 *
	 * @throws SemanticException if the operation cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> apply(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator operator,
			Expression statement)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Set<Type> leftTypes = analysis.getRuntimeTypesOf(state, left, statement);
		Set<Type> rightTypes = analysis.getRuntimeTypesOf(state, right, statement);
		AnalysisState<A> result = state.bottomExecution();
		if (leftTypes.stream().anyMatch(NumericOperands::isNumber)
				&& rightTypes.stream().anyMatch(NumericOperands::isNumber))
			result = analysis.smallStepSemantics(state,
					new BinaryExpression(statement.getStaticType(), left, right, operator, statement.getLocation()),
					statement);
		if (leftTypes.isEmpty() || rightTypes.isEmpty()
				|| !leftTypes.stream().allMatch(NumericOperands::isNumber)
				|| !rightTypes.stream().allMatch(NumericOperands::isNumber))
			// other operands (lists, tuples, objects defining the special
			// methods) are not modelled: their result is unknown
			result = result.lub(analysis.smallStepSemantics(state,
					new PushAny(Untyped.INSTANCE, statement.getLocation()), statement));
		return result;
	}

	/**
	 * Yields whether values of a type are Python numbers.
	 *
	 * @param type the type
	 *
	 * @return {@code true} for numeric types and booleans
	 */
	static boolean isNumber(
			Type type) {
		return type.isNumericType() || type.isBooleanType();
	}
}
