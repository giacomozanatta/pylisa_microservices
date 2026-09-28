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
 * The model of {@code rclpy.create_node(node_name, *, context, cli_args,
 * namespace, use_global_arguments, enable_rosout, start_parameter_services,
 * ...)}: it creates a new {@code rclpy.node.Node} object, initializes it as the
 * {@code Node} constructor does, and returns it.
 */
public class CreateNode extends RosNative {

	private static final int NODE_NAME = 0;

	private static final int CONTEXT = 1;

	private static final int CLI_ARGS = 2;

	private static final int NAMESPACE = 3;

	private static final int USE_GLOBAL_ARGUMENTS = 4;

	private static final int START_PARAMETER_SERVICES = 6;

	private static final int PARAMETER_OVERRIDES = 7;

	private static final int ALLOW_UNDECLARED_PARAMETERS = 8;

	private static final int DECLARE_PARAMETERS_FROM_OVERRIDES = 9;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected CreateNode(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "rclpy.create_node", parameters);
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
	public static CreateNode build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new CreateNode(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return EntityModels.create(state, RosTypes.NODE, callSite(),
				(created, node) -> created.forEachCombination(
						List.of(arguments[NODE_NAME], arguments[NAMESPACE], arguments[CONTEXT],
								arguments[START_PARAMETER_SERVICES], arguments[PARAMETER_OVERRIDES],
								arguments[CLI_ARGS], arguments[USE_GLOBAL_ARGUMENTS],
								arguments[ALLOW_UNDECLARED_PARAMETERS],
								arguments[DECLARE_PARAMETERS_FROM_OVERRIDES]),
						(current, values) -> ParameterModels.initialize(
								NodeModel.initialize(current, callSite(), node, values.get(0), values.get(1),
										values.get(2), values.get(3)),
								build, callSite(), node, values.get(4), values.get(5), values.get(6),
								values.get(7), values.get(8))
								.returning(node)));
	}
}
