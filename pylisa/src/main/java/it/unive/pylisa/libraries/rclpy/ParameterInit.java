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
 * The model of {@code rclpy.parameter.Parameter.__init__(self, name, type_,
 * value)}: it initializes a parameter object with its name, type and
 * value.
 */
public class ParameterInit extends RosNative {

	private static final int SELF = 0;

	private static final int NAME = 1;

	private static final int TYPE = 2;

	private static final int VALUE = 3;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected ParameterInit(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Parameter.__init__", parameters);
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
	public static ParameterInit build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new ParameterInit(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(
				List.of(arguments[SELF], arguments[NAME], arguments[TYPE], arguments[VALUE]),
				(current, values) -> ParameterModels.initializeParameter(current, build, values.get(0), values.get(1),
						values.get(2), values.get(3)));
	}
}
