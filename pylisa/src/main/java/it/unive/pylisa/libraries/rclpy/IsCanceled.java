package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.timer.Timer.is_canceled(self)}: it tells whether
 * the timer was canceled and not reset since.
 */
public class IsCanceled extends RosNative {

	private static final int SELF = 0;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected IsCanceled(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Timer.is_canceled", parameters);
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
	public static IsCanceled build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new IsCanceled(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEach(arguments[SELF], (current, timer) -> EntityModels.requireAlive(current, timer,
				(alive, t) -> {
					ModelState<A, D> canceled = alive.read(t, EntityModels.CANCELED);
					return canceled.forEach(canceled.values(), ModelState::returning);
				}));
	}
}
