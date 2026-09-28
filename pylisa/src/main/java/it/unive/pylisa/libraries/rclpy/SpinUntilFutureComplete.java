package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.spin_until_future_complete(node, future, executor=None, timeout_sec=None)}.
 */
public class SpinUntilFutureComplete extends RclpySpin {

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected SpinUntilFutureComplete(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "rclpy.spin_until_future_complete", 2, parameters);
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
	public static SpinUntilFutureComplete build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new SpinUntilFutureComplete(cfg, location, parameters);
	}
}
