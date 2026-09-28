package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.node.Node.get_fully_qualified_name()}, which returns the fully qualified name of a node as
 * computed when the node was created.
 */
public class GetFullyQualifiedName extends RosFieldAccessor {

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected GetFullyQualifiedName(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.get_fully_qualified_name", NodeModel.FULLY_QUALIFIED, parameters);
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
	public static GetFullyQualifiedName build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new GetFullyQualifiedName(cfg, location, parameters);
	}
}
