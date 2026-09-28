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
 * The model of {@code rclpy.node.Node.create_client(self, srv_type, srv_name,
 * *, qos_profile, callback_group)}: it creates a client of the node for the
 * service, whose name is resolved against the node.
 */
public class CreateClient extends RosNative {

	private static final int SELF = 0;

	private static final int SRV_TYPE = 1;

	private static final int SRV_NAME = 2;

	private static final int QOS_PROFILE = 3;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected CreateClient(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.create_client", parameters);
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
	public static CreateClient build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new CreateClient(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(
				List.of(arguments[SELF], arguments[SRV_TYPE], arguments[SRV_NAME], arguments[QOS_PROFILE]),
				(current, values) -> {
					SymbolicExpression self = values.get(0);
					SymbolicExpression srvName = values.get(2);
					return QosModels.serviceProfile(current, build, values.get(3),
							(profiled, qos) -> RosNames.resolve(profiled, build, self, srvName, RclpyExceptions.INVALID_SERVICE_NAME,
									(resolved, serviceName) -> EntityModels.client(resolved, callSite(), self,
											values.get(1), srvName, serviceName, qos)));
				});
	}
}
