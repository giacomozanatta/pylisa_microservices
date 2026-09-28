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
 * The model of a method of {@code rclpy.node.Node} that resolves a topic or
 * service name against the node ({@code resolve_topic_name(self, topic, *,
 * only_expand)} and {@code resolve_service_name(self, service, *,
 * only_expand)}): it returns the name that an entity created with that name
 * would have, and raises {@code RCLError} for a name that cannot be
 * resolved. Remapping is never applied, so {@code only_expand} makes no
 * difference.
 */
public abstract class RosNameResolver extends RosNative {

	private static final int SELF = 0;

	private static final int NAME = 1;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param name       the name of the modelled method
	 * @param parameters the arguments of the call
	 */
	protected RosNameResolver(
			CFG cfg,
			CodeLocation location,
			String name,
			Expression... parameters) {
		super(cfg, location, name, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		// the C function behind these methods reports every failure, invalid
		// names included, as RCLError
		return state.forEachCombination(List.of(arguments[SELF], arguments[NAME]),
				(current, values) -> NodeModel.requireAlive(current, values.get(0),
						(alive, node) -> RosNames.resolve(alive, build, node, values.get(1), RclpyExceptions.RCL_ERROR,
								ModelState::returning)));
	}
}
