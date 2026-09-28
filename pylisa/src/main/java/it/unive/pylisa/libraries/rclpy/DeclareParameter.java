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
 * The model of {@code rclpy.node.Node.declare_parameter(self, name, value,
 * descriptor, ignore_override)}: it declares a parameter of the node and
 * returns it. Its value is the default given by the program, or a value given
 * from outside the program where there may be one (see
 * {@link ParameterOverrides}).
 */
public class DeclareParameter extends RosNative {

	private static final int SELF = 0;

	private static final int NAME = 1;

	private static final int VALUE = 2;

	private static final int DESCRIPTOR = 3;

	private static final int IGNORE_OVERRIDE = 4;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected DeclareParameter(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.declare_parameter", parameters);
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
	public static DeclareParameter build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new DeclareParameter(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(
				List.of(arguments[SELF], arguments[NAME], arguments[VALUE], arguments[DESCRIPTOR], arguments[IGNORE_OVERRIDE]),
				(current, values) -> ParameterModels.declare(current, build, callSite(), values.get(0), values.get(1),
						values.get(2), values.get(3), values.get(4)));
	}
}
