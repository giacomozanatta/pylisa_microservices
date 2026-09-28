package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of a method of {@code rclpy.node.Node} that returns a value the
 * node stores, such as {@code get_name()}: the call returns the value of one
 * field of the node and has no other effect. rclpy keeps these values in its
 * C layer, so the call fails with {@code InvalidHandle} once the node is
 * destroyed; the models store them in fields of the node object when the
 * node is created.
 */
public abstract class NodeAccessor extends RosNative {

	private static final int SELF = 0;

	private final String field;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param name       the name of the modelled method
	 * @param field      the field whose value the method returns
	 * @param parameters the arguments of the call
	 */
	protected NodeAccessor(
			CFG cfg,
			CodeLocation location,
			String name,
			String field,
			Expression... parameters) {
		super(cfg, location, name, parameters);
		this.field = field;
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.forEach(arguments[SELF], (current, self) -> NodeModel.requireAlive(current, self,
				(alive, node) -> {
					ModelState<A, D> read = alive.read(node, field);
					return read.forEach(read.values(), ModelState::returning);
				}));
	}
}
