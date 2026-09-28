package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;

/**
 * The model of an rclpy callable whose result depends on things the analysis
 * cannot see, such as other processes of the ROS 2 graph, time, or the
 * internals of the ROS 2 client library: the call returns an unknown value and
 * is assumed to have no other effect on the program state.
 */
public class RosUnknownResult extends RosNative {

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected RosUnknownResult(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "rclpy-unknown-result", parameters);
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
	public static RosUnknownResult build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new RosUnknownResult(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.returning(new PushAny(Untyped.INSTANCE, callSite()));
	}
}
