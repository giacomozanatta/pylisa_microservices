package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.node.Node.add_on_set_parameters_callback(self,
 * callback)}: from now on, declaring or setting a parameter of the node may be
 * rejected by the callback, which is not run.
 */
public class AddSetParametersCallback extends RosNative {

	private static final int SELF = 0;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected AddSetParametersCallback(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.add_on_set_parameters_callback", parameters);
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
	public static AddSetParametersCallback build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new AddSetParametersCallback(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEach(arguments[SELF], (current, self) -> ParameterModels.addSetCallback(current, build, self));
	}
}
