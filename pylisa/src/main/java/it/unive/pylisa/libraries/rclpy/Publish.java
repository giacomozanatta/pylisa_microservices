package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.pylisa.cfg.type.PyClassType;
import it.unive.pylisa.cfg.type.PyExceptionType;
import java.util.List;
import java.util.Set;

/**
 * The model of {@code rclpy.publisher.Publisher.publish(self, msg)}: the
 * message is sent on the publisher's topic, which changes nothing in the
 * program state, and the call returns {@code None}. rclpy raises
 * {@code TypeError} for a message that is neither an instance of the
 * publisher's message type nor {@code bytes}; the types the message and the
 * message type may have decide where that happens.
 */
public class Publish extends RosNative {

	private static final int SELF = 0;

	private static final int MSG = 1;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected Publish(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Publisher.publish", parameters);
	}

	/**
	 * Builds the model of one call. This is the factory the library loader
	 * uses for native implementations.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 *
	 * @return the model
	 */
	public static Publish build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new Publish(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(List.of(arguments[SELF], arguments[MSG]), (current, values) -> {
			ModelState<A, D> type = current.read(values.get(0), "msg_type");
			return EntityModels.requireAlive(current, values.get(0),
					(alive, publisher) -> alive.forEach(type.values(), (typed, msgType) -> {
				Set<Type> classes = typed.runtimeTypes(msgType);
				Set<Type> messages = typed.runtimeTypes(values.get(1));
				ModelState<A, D> result = typed.unreachable();
				if (!certainlyRejected(classes, messages))
					result = result.lub(typed.returning(build.none()));
				if (!certainlyAccepted(classes, messages))
					result = result.lub(typed.raise(PyExceptionType.TYPE_ERROR));
				return result;
			}));
		});
	}

	/**
	 * Yields whether every possible message is an instance of every possible
	 * message type.
	 */
	private static boolean certainlyAccepted(
			Set<Type> classes,
			Set<Type> messages) {
		if (!areClasses(classes) || messages.isEmpty())
			return false;
		for (Type message : messages) {
			if (!message.isPointerType())
				return false;
			Type object = message.asPointerType().getInnerType();
			if (!isBytes(object) && !classes.stream().allMatch(object::canBeAssignedTo))
				return false;
		}
		return true;
	}

	/**
	 * Yields whether no possible message is an instance of a possible message
	 * type, nor bytes: plain values, and objects of unrelated classes.
	 */
	private static boolean certainlyRejected(
			Set<Type> classes,
			Set<Type> messages) {
		if (!areClasses(classes) || messages.isEmpty())
			return false;
		for (Type message : messages) {
			if (message.isNumericType() || message.isBooleanType() || message.isStringType() || message.isNullType())
				continue;
			if (!message.isPointerType())
				return false;
			Type object = message.asPointerType().getInnerType();
			if (!(object instanceof PyClassType) || isBytes(object)
					|| classes.stream().anyMatch(object::canBeAssignedTo))
				return false;
		}
		return true;
	}

	private static boolean areClasses(
			Set<Type> types) {
		return !types.isEmpty() && types.stream().allMatch(PyClassType.class::isInstance);
	}

	private static boolean isBytes(
			Type type) {
		return type.toString().startsWith("builtins.bytes");
	}
}