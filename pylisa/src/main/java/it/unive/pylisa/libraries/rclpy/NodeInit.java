package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import java.util.List;

/**
 * The model of {@code rclpy.node.Node.__init__(self, node_name, *, context,
 * cli_args, namespace, use_global_arguments, enable_rosout,
 * start_parameter_services, ...)}: it initializes {@code self} as a ROS 2 node.
 * This is also what a subclass of {@code Node} runs through
 * {@code super().__init__(...)}.
 */
public class NodeInit extends RosNative {

	private static final int SELF = 0;

	private static final int NODE_NAME = 1;

	private static final int CONTEXT = 2;

	private static final int NAMESPACE = 4;

	private static final int START_PARAMETER_SERVICES = 7;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected NodeInit(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.__init__", parameters);
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
	public static NodeInit build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new NodeInit(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.forEachCombination(
				List.of(arguments[SELF], arguments[NODE_NAME], arguments[NAMESPACE], arguments[CONTEXT],
						arguments[START_PARAMETER_SERVICES]),
				(current, values) -> NodeModel.initialize(current, callSite(), values.get(0), values.get(1),
						values.get(2), values.get(3), values.get(4)));
	}
}
