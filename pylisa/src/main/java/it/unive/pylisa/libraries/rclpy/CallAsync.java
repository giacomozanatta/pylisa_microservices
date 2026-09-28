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
 * The model of {@code rclpy.client.Client.call_async(self, request)}: it
 * returns a new future for the response, which refers to the client in
 * {@value #CLIENT_FIELD} and to the request in {@value #REQUEST_FIELD}. The
 * response depends on another process: the outcome of the future is unknown.
 */
public class CallAsync extends RosNative {

	private static final int SELF = 0;

	private static final int REQUEST = 1;

	/**
	 * The field of a future that refers to the client that sent the request.
	 */
	static final String CLIENT_FIELD = "$client";

	/**
	 * The field of a future that refers to the request.
	 */
	static final String REQUEST_FIELD = "$request";

	/**
	 * Builds the model of one call.
	 *
	 * @param cfg        the CFG the call belongs to
	 * @param location   the location of the call
	 * @param parameters the arguments of the call
	 */
	protected CallAsync(
			CFG cfg,
			CodeLocation location,
			Expression... parameters) {
		super(cfg, location, "Client.call_async", parameters);
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
	public static CallAsync build(
			CFG cfg,
			CodeLocation location,
			Expression[] parameters) {
		return new CallAsync(cfg, location, parameters);
	}

	@Override
	protected <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> model(
			ModelState<A, D> state,
			ExpressionSet[] arguments)
			throws SemanticException {
		return state.forEachCombination(List.of(arguments[SELF], arguments[REQUEST]),
				(current, values) -> EntityModels.create(current, RosTypes.FUTURE, callSite(),
						(created, future) -> created
								.write(future, CLIENT_FIELD, values.get(0))
								.write(future, REQUEST_FIELD, values.get(1))
								.returning(future)));
	}
}
