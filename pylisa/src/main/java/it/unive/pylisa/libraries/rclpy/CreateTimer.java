package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.pylisa.cfg.type.PyExceptionType;
import java.util.List;

/**
 * The model of {@code rclpy.node.Node.create_timer(self, timer_period_sec,
 * callback, callback_group, clock)}: it creates a timer of the node whose
 * period is {@code int(float(timer_period_sec) * 1e9)} nanoseconds, as rclpy
 * computes it; rcl rejects a negative period, and a period that is not a
 * finite number within 64 bits fails before reaching rcl. The callback is
 * stored, not run.
 */
public class CreateTimer extends RosNative {

	private static final int SELF = 0;

	private static final int PERIOD = 1;

	private static final int CALLBACK = 2;

	private static final double NANOSECONDS_PER_SECOND = 1e9;

	private static final double TWO_TO_THE_63 = 0x1p63;

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected CreateTimer(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Node.create_timer", parameters);
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
	public static CreateTimer build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new CreateTimer(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		Expressions build = new Expressions(callSite());
		return state.forEachCombination(List.of(arguments[SELF], arguments[PERIOD], arguments[CALLBACK]),
				(current, values) -> {
					SymbolicExpression nanoseconds = build.times(values.get(1), build.real(NANOSECONDS_PER_SECOND));
					// the period is passed to C as a 64-bit integer: a float
					// that is not a number, infinite or out of that range
					// makes int() or the conversion fail
					SymbolicExpression representable = build.and(
							build.lessOrEqual(build.real(-TWO_TO_THE_63), nanoseconds),
							build.lessThan(nanoseconds, build.real(TWO_TO_THE_63)));
					return current.branch(representable,
							(converted, c) -> converted.branch(build.lessThan(build.real(-1), nanoseconds),
									(accepted, c1) -> accepted.branch(build.lessThan(nanoseconds, build.integer(0)),
											// int() truncates toward zero
											(truncated, c2) -> EntityModels.timer(truncated, build, callSite(),
													values.get(0), build.integer(0), values.get(2)),
											(positive, c2) -> EntityModels.timer(positive, build, callSite(),
													values.get(0), build.floor(nanoseconds), values.get(2))),
									// rcl rejects a negative period
									(negative, c1) -> negative.raise(RclpyExceptions.RCL_ERROR)),
							(failed, c) -> failed.raise(PyExceptionType.VALUE_ERROR)
									.lub(failed.raise(PyExceptionType.OVERFLOW_ERROR))
									.lub(failed.raise(PyExceptionType.TYPE_ERROR)));
				});
	}
}
