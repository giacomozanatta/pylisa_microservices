package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;

/**
 * What executors do to the program state. Adding a node to an executor, or
 * spinning a node, records the executor in the {@value #EXECUTOR} attribute of
 * the node, as rclpy does. The callbacks an executor would run are not
 * executed: their registration is all the state records.
 */
final class ExecutorModel {

	/**
	 * The attribute of a node that refers to the executor it was added to.
	 */
	static final String EXECUTOR = "executor";

	/**
	 * The name of the variable holding the executor that rclpy creates on
	 * first use for the {@code rclpy.spin*} functions.
	 */
	static final String GLOBAL_EXECUTOR = "$rclpy::__executor";

	/**
	 * {@code rclpy.executors.SingleThreadedExecutor}, the class of the global
	 * executor.
	 */
	static final String SINGLE_THREADED = "rclpy.executors.SingleThreadedExecutor";

	private ExecutorModel() {
	}

	/**
	 * Yields the global executor, creating it at this call when it may not
	 * exist yet.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param site  the location of the call
	 *
	 * @return the state whose computed value is the global executor
	 *
	 * @throws SemanticException if the executor cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> globalExecutor(
			ModelState<A, D> state,
			CodeLocation site)
			throws SemanticException {
		Expressions build = new Expressions(site);
		GlobalVariable global = new GlobalVariable(Untyped.INSTANCE, GLOBAL_EXECUTOR, site);
		return state.branch(build.isNone(global),
				(missing, condition) -> EntityModels.create(missing, SINGLE_THREADED,
						new TaggedLocation(site, "global-executor"),
						(created, executor) -> created.assign(global, executor).returning(executor)),
				(existing, condition) -> existing.returning(global));
	}

	/**
	 * Records that a node is spun by an executor: the given one or, when it is
	 * {@code None}, the global executor.
	 *
	 * @param <A>      the kind of abstract state
	 * @param <D>      the kind of abstract domain
	 * @param state    the state
	 * @param site     the location of the call
	 * @param node     the node
	 * @param executor the executor given by the program, or {@code None}
	 *
	 * @return the state after the call, with {@code None} as computed value
	 *
	 * @throws SemanticException if the effect cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> spin(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression executor)
			throws SemanticException {
		Expressions build = new Expressions(site);
		return state.branch(build.isNone(executor),
				(implicit, condition) -> {
					ModelState<A, D> global = globalExecutor(implicit, site);
					return global.forEach(global.values(), (current, found) -> current.write(node, EXECUTOR, found));
				},
				(explicit, condition) -> explicit.write(node, EXECUTOR, executor))
				.returning(build.none());
	}
}
