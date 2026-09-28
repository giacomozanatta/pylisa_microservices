package it.unive.pylisa.libraries.rclpy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The values given to parameters from outside the analysed program (launch
 * files, {@code --ros-args -p}, {@code --params-file}). rclpy's
 * {@code declare_parameter} uses such a value, when there is one, instead of
 * the default the program gives. Which values exist depends on how the
 * program is deployed, not on its code: the client of the analysis supplies
 * them with {@link #use}. Unless it does, any parameter may have any value
 * of its type, which is the sound choice.
 */
public interface ParameterOverrides {

	/**
	 * A value known to be given to a parameter from outside the program.
	 *
	 * @param node  the fully qualified name of the node
	 * @param name  the name of the parameter
	 * @param value the value: a string, a boolean, an integer or a float
	 */
	record Known(String node, String name, Object value) {

		/**
		 * Builds the override.
		 *
		 * @param node  the fully qualified name of the node
		 * @param name  the name of the parameter
		 * @param value the value
		 */
		public Known {
			Objects.requireNonNull(node);
			Objects.requireNonNull(name);
			Objects.requireNonNull(value);
		}
	}

	/**
	 * Yields the values known to be given from outside.
	 *
	 * @return the known values
	 */
	List<Known> known();

	/**
	 * Yields whether parameters other than the {@link #known()} ones may be
	 * given a value from outside.
	 *
	 * @return {@code true} if they may
	 */
	boolean othersMayBeOverridden();

	/**
	 * Any parameter may be given any value from outside.
	 */
	ParameterOverrides UNKNOWN = of(List.of(), true);

	/**
	 * No parameter is given a value from outside.
	 */
	ParameterOverrides NONE = of(List.of(), false);

	/**
	 * Yields the overrides with the given known values.
	 *
	 * @param known                 the values known to be given from outside
	 * @param othersMayBeOverridden whether other parameters may be given
	 *                                  values from outside
	 *
	 * @return the overrides
	 */
	static ParameterOverrides of(
			List<Known> known,
			boolean othersMayBeOverridden) {
		List<Known> values = List.copyOf(known);
		return new ParameterOverrides() {

			@Override
			public List<Known> known() {
				return values;
			}

			@Override
			public boolean othersMayBeOverridden() {
				return othersMayBeOverridden;
			}
		};
	}

	/**
	 * Sets the overrides used by the analyses that start from now on.
	 *
	 * @param overrides the overrides
	 */
	static void use(
			ParameterOverrides overrides) {
		Holder.CURRENT.set(Objects.requireNonNull(overrides));
	}

	/**
	 * Yields the overrides in use, {@link #UNKNOWN} unless others were set.
	 *
	 * @return the overrides
	 */
	static ParameterOverrides current() {
		return Holder.CURRENT.get();
	}

	/**
	 * Holds the overrides in use.
	 */
	final class Holder {

		private static final AtomicReference<ParameterOverrides> CURRENT = new AtomicReference<>(UNKNOWN);

		private Holder() {
		}
	}
}
