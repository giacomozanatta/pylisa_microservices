package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.node.Node.resolve_service_name(self, service, *,
 * only_expand)}, which resolves a service name against the node.
 */
public class ResolveServiceName extends RosNameResolver {

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected ResolveServiceName(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.resolve_service_name", parameters);
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
	public static ResolveServiceName build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new ResolveServiceName(cfg, location, parameters);
	}
}
