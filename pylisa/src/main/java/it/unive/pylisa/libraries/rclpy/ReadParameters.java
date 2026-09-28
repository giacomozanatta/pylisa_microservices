package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of the methods of {@code rclpy.node.Node} that read parameters in
 * ways whose results are not tracked ({@code get_parameters},
 * {@code get_parameters_by_prefix}, {@code get_parameter_type(s)},
 * {@code describe_parameter(s)}): the result is unknown, and the call may
 * raise for undeclared or uninitialized parameters.
 */
public class ReadParameters extends RosNative {



	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected ReadParameters(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.get_parameters", parameters);
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
	public static ReadParameters build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new ReadParameters(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return ParameterModels.readUntracked(state, build);
	}
}
