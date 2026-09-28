package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * What the rclpy functions that manage a context ({@code init},
 * {@code shutdown}, {@code ok}) do to the program state. A context is an
 * object with the boolean field {@value NodeModel#CONTEXT_OK}; the functions
 * work on the context they receive or, when they receive {@code None}, on
 * rclpy's default context.
 */
final class ContextModel {

	private ContextModel() {
	}

	/**
	 * Initializes a context: the default one, created by this call, when
	 * {@code context} is {@code None}, the given one otherwise.
	 *
	 * @param <A>     the kind of abstract state
	 * @param <D>     the kind of abstract domain
	 * @param state   the state
	 * @param site    the location of the call
	 * @param context the context given by the program, or {@code None}
	 *
	 * @return the state after the initialization, with {@code None} as
	 *             computed value
	 *
	 * @throws SemanticException if the initialization cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> init(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression context)
			throws SemanticException {
		Expressions build = new Expressions(site);
		return state.branch(build.isNone(context),
				(implicit, condition) -> EntityModels.create(implicit, RosTypes.CONTEXT, site,
						(created, fresh) -> created
								.write(fresh, NodeModel.CONTEXT_OK, build.bool(true))
								.assign(NodeModel.defaultContext(build), fresh)),
				(explicit, condition) -> explicit.write(context, NodeModel.CONTEXT_OK, build.bool(true)))
				.returning(build.none());
	}

	/**
	 * Shuts a context down.
	 *
	 * @param <A>     the kind of abstract state
	 * @param <D>     the kind of abstract domain
	 * @param state   the state
	 * @param site    the location of the call
	 * @param context the context given by the program, or {@code None}
	 *
	 * @return the state after the shutdown, with {@code None} as computed
	 *             value
	 *
	 * @throws SemanticException if the shutdown cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> shutdown(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression context)
			throws SemanticException {
		Expressions build = new Expressions(site);
		return onTarget(state, build, context,
				(current, target) -> current.write(target, NodeModel.CONTEXT_OK, build.bool(false)))
				.returning(build.none());
	}

	/**
	 * Tells whether a context is initialized and not shut down.
	 *
	 * @param <A>     the kind of abstract state
	 * @param <D>     the kind of abstract domain
	 * @param state   the state
	 * @param site    the location of the call
	 * @param context the context given by the program, or {@code None}
	 *
	 * @return the state whose computed value is the answer
	 *
	 * @throws SemanticException if the answer cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> ok(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression context)
			throws SemanticException {
		return onTarget(state, new Expressions(site), context, (current, target) -> {
			ModelState<A, D> read = current.read(target, NodeModel.CONTEXT_OK);
			return read.forEach(read.values(), ModelState::returning);
		});
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> onTarget(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression context,
			ModelState.Step<A, D, SymbolicExpression> action)
			throws SemanticException {
		return state.branch(build.isNone(context),
				(implicit, condition) -> action.apply(implicit, NodeModel.defaultContext(build)),
				(explicit, condition) -> action.apply(explicit, context));
	}
}
