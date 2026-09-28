package it.unive.pylisa.libraries.rclpy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The values given to parameters from outside the analysed program: when it
 * starts, by the command line of the process (launch files,
 * {@code --ros-args -p}, {@code --params-file}), which rclpy's
 * {@code declare_parameter} uses instead of the default the program gives;
 * and while it runs, by other nodes calling the parameter services of a node
 * that is being spun. Which values exist depends on how the
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
	 * Yields whether other nodes may change the parameters of a node, through
	 * its parameter services, once the node is spun or added to an executor.
	 *
	 * @return {@code true} if they may
	 */
	boolean remoteChangesPossible();

	/**
	 * Any parameter may be given any value from outside, when the program
	 * starts and while it runs.
	 */
	ParameterOverrides UNKNOWN = of(List.of(), true, true);

	/**
	 * No parameter is given a value from outside, neither when the program
	 * starts nor while it runs.
	 */
	ParameterOverrides NONE = of(List.of(), false, false);

	/**
	 * Yields the overrides with the given known values.
	 *
	 * @param known                 the values known to be given from outside
	 *                                  when the program starts
	 * @param othersMayBeOverridden whether other parameters may be given
	 *                                  values from outside when the program
	 *                                  starts
	 * @param remoteChangesPossible whether other nodes may change parameters
	 *                                  while the program runs
	 *
	 * @return the overrides
	 */
	static ParameterOverrides of(
			List<Known> known,
			boolean othersMayBeOverridden,
			boolean remoteChangesPossible) {
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

			@Override
			public boolean remoteChangesPossible() {
				return remoteChangesPossible;
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
