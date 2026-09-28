package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.node.Node.resolve_topic_name(self, topic, *,
 * only_expand)}, which resolves a topic name against the node.
 */
public class ResolveTopicName extends RosNameResolver {

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected ResolveTopicName(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.resolve_topic_name", parameters);
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
	public static ResolveTopicName build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new ResolveTopicName(cfg, location, parameters);
	}
}
