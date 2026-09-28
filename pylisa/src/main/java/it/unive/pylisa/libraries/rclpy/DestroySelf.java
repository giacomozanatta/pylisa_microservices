package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of the {@code destroy(self)} method of an rclpy entity (a
 * publisher, a subscription, a timer, a service, a client, a guard
 * condition): the entity becomes destroyed, and the call returns
 * {@code None}.
 */
public class DestroySelf extends RosNative {

	private static final int SELF = 0;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected DestroySelf(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Entity.destroy", parameters);
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
	public static DestroySelf build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new DestroySelf(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEach(arguments[SELF],
				(current, entity) -> current.write(entity, EntityModels.DESTROYED, build.bool(true))
						.returning(build.none()));
	}
}
