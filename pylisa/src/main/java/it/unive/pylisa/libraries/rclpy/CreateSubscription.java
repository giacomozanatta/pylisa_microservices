package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.List;

/**
 * The model of {@code rclpy.node.Node.create_subscription(self, msg_type,
 * topic, callback, qos_profile, *, ..., raw)}: it creates a subscription of
 * the node to the topic, resolved against the node. The callback is stored,
 * not run.
 */
public class CreateSubscription extends RosNative {

	private static final int SELF = 0;

	private static final int MSG_TYPE = 1;

	private static final int TOPIC = 2;

	private static final int CALLBACK = 3;

	private static final int QOS_PROFILE = 4;

	private static final int RAW = 8;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected CreateSubscription(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.create_subscription", parameters);
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
	public static CreateSubscription build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new CreateSubscription(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(
				List.of(arguments[SELF], arguments[MSG_TYPE], arguments[TOPIC], arguments[CALLBACK],
						arguments[QOS_PROFILE], arguments[RAW]),
				(current, values) -> {
					SymbolicExpression self = values.get(0);
					SymbolicExpression topic = values.get(2);
					SymbolicExpression qos = values.get(4);
					return QosModels.depth(current, build, qos,
							(checked, depth) -> RosNames.resolve(checked, build, self, topic, RclpyExceptions.INVALID_TOPIC_NAME,
									(resolved, topicName) -> EntityModels.subscription(resolved, callSite(), self,
											values.get(1), topic, topicName, values.get(3), qos, depth,
											values.get(5))));
				});
	}
}
