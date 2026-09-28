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
 * The model of the {@code rclpy.spin*} functions: the node is spun by the
 * given executor or by the global one. The callbacks the executor would run
 * are not executed.
 */
public abstract class RclpySpin extends RosNative {

	private static final int NODE = 0;

	private final int executorIndex;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg           the CFG the call belongs to
	 * @param location      the location of the call
	 * @param name          the name of the modelled function
	 * @param executorIndex the position of the executor among the parameters
	 * @param parameters    the arguments of the call
	 */
	protected RclpySpin(
			CFG cfg,
			CodeLocation location,
			String name,
			int executorIndex,
			Expression... parameters) {
		super(cfg, location, name, parameters);
		this.executorIndex = executorIndex;
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.forEachCombination(List.of(arguments[NODE], arguments[executorIndex]),
				(current, values) -> ExecutorModel.spin(current, callSite(), values.get(0), values.get(1)));
	}
}
