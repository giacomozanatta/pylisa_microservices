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
 * The model of {@code rclpy.node.Node.create_service(self, srv_type, srv_name,
 * callback, *, qos_profile, callback_group)}: it creates a service of the
 * node, whose name is resolved against the node. The callback is stored, not
 * run.
 */
public class CreateService extends RosNative {

	private static final int SELF = 0;

	private static final int SRV_TYPE = 1;

	private static final int SRV_NAME = 2;

	private static final int CALLBACK = 3;

	private static final int QOS_PROFILE = 4;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected CreateService(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.create_service", parameters);
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
	public static CreateService build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new CreateService(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(
				List.of(arguments[SELF], arguments[SRV_TYPE], arguments[SRV_NAME], arguments[CALLBACK],
						arguments[QOS_PROFILE]),
				(current, values) -> {
					SymbolicExpression self = values.get(0);
					SymbolicExpression srvName = values.get(2);
					return QosModels.serviceProfile(current, build, values.get(4),
							(profiled, qos) -> RosNames.resolve(profiled, build, self, srvName, RclpyExceptions.INVALID_SERVICE_NAME,
									(resolved, serviceName) -> EntityModels.service(resolved, build, callSite(),
											self, values.get(1), srvName, serviceName, values.get(3), qos)));
				});
	}
}
