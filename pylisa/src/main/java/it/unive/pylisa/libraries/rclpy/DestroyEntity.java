package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.List;

/**
 * The model of the methods of {@code rclpy.node.Node} that destroy one of its
 * entities ({@code destroy_publisher(self, publisher)},
 * {@code destroy_subscription}, {@code destroy_client},
 * {@code destroy_service}, {@code destroy_timer},
 * {@code destroy_guard_condition}): an entity of this node that is not
 * destroyed yet becomes destroyed, and the call returns {@code True}; for any
 * other entity, or on a destroyed node, it returns {@code False}.
 */
public class DestroyEntity extends RosNative {

	private static final int SELF = 0;

	private static final int ENTITY = 1;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected DestroyEntity(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.destroy_publisher", parameters);
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
	public static DestroyEntity build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new DestroyEntity(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(List.of(arguments[SELF], arguments[ENTITY]), (current, values) -> {
			SymbolicExpression node = values.get(0);
			SymbolicExpression entity = values.get(1);
			if (current.objects(entity).isEmpty())
				// not an entity the models created
				return current.returning(build.unknown(BoolType.INSTANCE));
			ModelState<A, D> destroyed = current.read(node, EntityModels.DESTROYED);
			return current.forEach(destroyed.values(), (checked, nodeDestroyed) -> checked.branch(nodeDestroyed,
					// a destroyed node has no entities left
					(gone, c) -> gone.returning(build.bool(false)),
					(present, c) -> {
						ModelState<A, D> owner = present.read(entity, EntityModels.NODE);
						return present.forEach(owner.values(), (owned, entityNode) -> {
							Satisfiability same = owned.sameObject(entityNode, node);
							ModelState<A, D> result = owned.unreachable();
							if (same != Satisfiability.SATISFIED)
								result = result.lub(owned.returning(build.bool(false)));
							if (same != Satisfiability.NOT_SATISFIED)
								result = result.lub(destroyOwned(owned, build, entity));
							return result;
						});
					}));
		});
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> destroyOwned(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression entity)
			throws SemanticException {
		ModelState<A, D> destroyed = state.read(entity, EntityModels.DESTROYED);
		// an entity destroyed before is no longer in the lists of its node
		return state.forEach(destroyed.values(), (checked, flag) -> checked.branch(flag,
				(gone, c) -> gone.returning(build.bool(false)),
				(present, c) -> present.write(entity, EntityModels.DESTROYED, build.bool(true))
						.returning(build.bool(true))));
	}
}
