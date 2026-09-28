package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.shutdown(*, context=None, ...)}: it shuts the given context down, or else the default one. It also models {@code rclpy.try_shutdown}.
 */
public class RclpyShutdown extends RosNative {

	private static final int CONTEXT = 0;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected RclpyShutdown(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "rclpy.shutdown", parameters);
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
	public static RclpyShutdown build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new RclpyShutdown(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.forEach(arguments[CONTEXT], (current, context) -> ContextModel.shutdown(current, callSite(), context));
	}
}
