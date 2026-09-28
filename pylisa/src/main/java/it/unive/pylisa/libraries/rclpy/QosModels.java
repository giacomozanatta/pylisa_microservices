package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.pylisa.cfg.type.PyExceptionType;
import java.util.Set;

/**
 * What rclpy does with the quality of service given when an entity is
 * created. For a publisher or a subscription, an integer {@code n} stands for
 * a profile that keeps the last {@code n} messages, and a negative one raises
 * {@code ValueError}; a {@code QoSProfile} object carries its own depth, which
 * the models do not track; anything else raises {@code TypeError}. Services and clients take a profile, or
 * {@code None} for rclpy's default.
 */
final class QosModels {

	private QosModels() {
	}

	/**
	 * Computes the history depth of a quality of service.
	 *
	 * @param <A>       the kind of abstract state
	 * @param <D>       the kind of abstract domain
	 * @param state     the state
	 * @param build     the factory of expressions
	 * @param profile   the quality of service given by the program
	 * @param withDepth what to do with the depth
	 *
	 * @return the join of the outcomes
	 *
	 * @throws SemanticException if the depth cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> depth(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression profile,
			ModelState.Step<A, D, SymbolicExpression> withDepth)
			throws SemanticException {
		Set<Type> types = state.runtimeTypes(profile);
		boolean unknown = types.isEmpty() || types.stream().anyMatch(Type::isUntyped);
		boolean mayBeInteger = unknown || types.stream().anyMatch(QosModels::isInteger);
		// any object may be a QoSProfile, since the class is not tracked
		boolean mayBeProfile = unknown || types.stream().anyMatch(QosModels::isObject);
		// None, strings, floats and other plain values are rejected
		boolean mayBeOther = unknown || types.stream().anyMatch(t -> !isInteger(t) && !isObject(t));
		ModelState<A, D> result = state.unreachable();
		if (mayBeInteger)
			result = result.lub(state.branch(build.lessThan(profile, build.integer(0)),
					(negative, condition) -> negative.raise(PyExceptionType.VALUE_ERROR),
					(depth, condition) -> withDepth.apply(depth, profile)));
		if (mayBeProfile)
			result = result.lub(withDepth.apply(state, build.unknown(Int32Type.INSTANCE)));
		if (mayBeOther)
			result = result.lub(state.raise(PyExceptionType.TYPE_ERROR));
		return result;
	}

	/**
	 * Computes the quality of service of a service or a client: the one given
	 * by the program, or rclpy's default for services when the program gives
	 * {@code None}. The default is an rclpy object the models do not track, so
	 * it is an unknown value.
	 *
	 * @param <A>         the kind of abstract state
	 * @param <D>         the kind of abstract domain
	 * @param state       the state
	 * @param build       the factory of expressions
	 * @param profile     the quality of service given by the program
	 * @param withProfile what to do with the quality of service
	 *
	 * @return the join of the outcomes
	 *
	 * @throws SemanticException if the quality of service cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> serviceProfile(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression profile,
			ModelState.Step<A, D, SymbolicExpression> withProfile)
			throws SemanticException {
		return state.ifNone(profile,
				(byDefault, condition) -> withProfile.apply(byDefault, build.unknown()),
				(given, condition) -> withProfile.apply(given, profile));
	}

	private static boolean isObject(
			Type type) {
		return !type.isNullType() && (type.isPointerType() || type.isInMemoryType());
	}

	private static boolean isInteger(
			Type type) {
		return type.isBooleanType() || (type.isNumericType() && type.asNumericType().isIntegral());
	}
}
