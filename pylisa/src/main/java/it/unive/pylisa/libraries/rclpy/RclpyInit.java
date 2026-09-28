package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.init(*, context=None, ...)}: it initializes the given context, or else a new default context.
 */
public class RclpyInit extends RosNative {

	private static final int ARGS = 0;

	private static final int CONTEXT = 1;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected RclpyInit(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "rclpy.init", parameters);
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
	public static RclpyInit build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new RclpyInit(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.forEachCombination(java.util.List.of(arguments[CONTEXT], arguments[ARGS]),
				(current, values) -> ContextModel.init(current, callSite(), values.get(0), values.get(1)));
	}
}
