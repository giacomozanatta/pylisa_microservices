package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of {@code rclpy.node.Node.destroy_node(self)}: the node and, with
 * it, every entity it owns become destroyed; their handles are freed, so
 * using them raises {@code InvalidHandle}. Destroying a node twice frees its
 * handle twice, which may raise.
 */
public class DestroyNode extends RosNative {

	private static final int SELF = 0;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected DestroyNode(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.destroy_node", parameters);
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
	public static DestroyNode build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new DestroyNode(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEach(arguments[SELF], (current, node) -> {
			ModelState<A, D> destroyed = current.read(node, EntityModels.DESTROYED);
			return current.forEach(destroyed.values(), (checked, flag) -> checked.branch(flag,
					(gone, c) -> gone.returning(build.none()).lub(gone.raise(RclpyExceptions.INVALID_HANDLE)),
					(present, c) -> present.write(node, EntityModels.DESTROYED, build.bool(true))
							.returning(build.none())));
		});
	}
}
