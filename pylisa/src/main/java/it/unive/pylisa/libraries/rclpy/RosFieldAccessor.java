package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * The model of an rclpy method that returns a value the object stores, such
 * as {@code Node.get_name()}: the call returns the value of one field of the
 * receiver and has no other effect. rclpy keeps many of these values in its C
 * layer; the models store them in fields of the Python object when the object
 * is created.
 */
public abstract class RosFieldAccessor extends RosNative {

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
	protected RosFieldAccessor(
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
		return state.forEach(arguments[SELF], (current, self) -> {
			ModelState<A, D> read = current.read(self, field);
			return read.forEach(read.values(), ModelState::returning);
		});
	}
}
