package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.pylisa.cfg.type.PyExceptionType;

/**
 * The model of {@code rclpy.node.Node.remove_on_set_parameters_callback(self,
 * callback)}: it raises {@code ValueError} for a callback that was not
 * registered. Callbacks still registered keep checking parameters, so the
 * node is not considered free of them.
 */
public class RemoveSetParametersCallback extends RosNative {



	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected RemoveSetParametersCallback(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.remove_on_set_parameters_callback", parameters);
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
	public static RemoveSetParametersCallback build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new RemoveSetParametersCallback(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.returning(build.none()).lub(state.raise(PyExceptionType.VALUE_ERROR));
	}
}
