package it.unive.pylisa.testing;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.string.BoundedStringSet;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.pylisa.analysis.constants.ConstantPropagation;

/**
 * Reads the abstract value that a value domain associates with an identifier,
 * as a domain-independent {@link Val}. There is one reader per value domain a
 * test configuration can use: this is the only place where tests depend on the
 * shape of a specific value domain.
 */
@FunctionalInterface
public interface ValueReader {

	/**
	 * Reads the value of an identifier.
	 *
	 * @param valueState the value component of an analysis state
	 * @param identifier the identifier to read (a variable or a heap location)
	 *
	 * @return the value, or {@code null} if the state holds bottom for the
	 *             identifier (no execution reaches this point with a value
	 *             for it)
	 *
	 * @throws IllegalArgumentException if {@code valueState} does not come
	 *                                      from the domain this reader is for
	 */
	Val read(
			Lattice<?> valueState,
			Identifier identifier);

	/**
	 * Yields the reader for pylisa's {@link ConstantPropagation}.
	 *
	 * @return the reader
	 */
	static ValueReader constantPropagation() {
		return (
				valueState,
				identifier) -> {
			if (!(valueState instanceof ValueEnvironment<?> environment))
				throw new IllegalArgumentException("Not a constant propagation state: " + valueState);
			Lattice<?> value = environment.getState(identifier);
			if (!(value instanceof ConstantPropagation constant))
				throw new IllegalArgumentException("Not a constant propagation value: " + value);
			if (constant.isBottom())
				return null;
			if (constant.isTop())
				return Val.top();
			return Val.exact(constant.getConstant());
		};
	}

	/**
	 * Yields the reader for the product of pylisa's {@link ConstantPropagation}
	 * with a {@link BoundedStringSet}. The two components describe the same
	 * values, so the more precise of their two readings is returned: an exact
	 * constant, else a finite set of strings, else unknown.
	 *
	 * @return the reader
	 */
	static ValueReader constantPropagationWithStringSets() {
		ValueReader constants = constantPropagation();
		return (
				valueState,
				identifier) -> {
			if (!(valueState instanceof ValueLatticeProduct<?, ?> product))
				throw new IllegalArgumentException("Not a product state: " + valueState);
			Val constant = constants.read(product.first, identifier);
			if (!(constant instanceof Val.Top))
				return constant;
			if (!(product.second instanceof ValueEnvironment<?> environment))
				throw new IllegalArgumentException("Not a string set state: " + product.second);
			Lattice<?> value = environment.getState(identifier);
			if (!(value instanceof BoundedStringSet.BSS strings))
				throw new IllegalArgumentException("Not a string set value: " + value);
			if (strings.isTop() || strings.isBottom() || strings.elements().isEmpty())
				return constant;
			return Val.oneOf(strings.elements());
		};
	}
}
