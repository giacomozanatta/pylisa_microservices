package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import java.util.List;

/**
 * The model of the methods of {@code rclpy.node.Node} that declare or set
 * several parameters at once ({@code declare_parameters},
 * {@code set_parameters}, {@code set_parameters_atomically}, and
 * {@code set_descriptor}, which may also set a value): the parameters
 * of the node become unknown, and the call may raise any of the parameter
 * exceptions of these methods.
 */
public class ChangeParameters extends RosNative {

	private static final int SELF = 0;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected ChangeParameters(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.set_parameters", parameters);
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
	public static ChangeParameters build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new ChangeParameters(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(
				List.of(arguments[SELF]),
				(current, values) -> ParameterModels.changeUntracked(current, build, values.get(0)));
	}
}
