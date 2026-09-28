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
 * The model of {@code rclpy.node.Node.create_guard_condition(self, callback,
 * callback_group)}: it creates a guard condition of the node. The callback is
 * stored, not run.
 */
public class CreateGuardCondition extends RosNative {

	private static final int SELF = 0;

	private static final int CALLBACK = 1;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected CreateGuardCondition(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.create_guard_condition", parameters);
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
	public static CreateGuardCondition build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new CreateGuardCondition(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.forEachCombination(List.of(arguments[SELF], arguments[CALLBACK]),
				(current, values) -> EntityModels.guardCondition(current, callSite(), values.get(0),
						values.get(1)));
	}
}
