package it.unive.pylisa.libraries.rclpy;

import it.unive.pylisa.cfg.type.PyClassType;
import java.util.Optional;

/**
 * The rclpy classes whose instances the models create. A class is available
 * once the module that defines it has been imported by the analysed program,
 * directly or through the modules it imports.
 */
final class RosTypes {

	/**
	 * {@code rclpy.context.Context}.
	 */
	static final String CONTEXT = "rclpy.context.Context";

	/**
	 * {@code rclpy.node.Node}.
	 */
	static final String NODE = "rclpy.node.Node";

	/**
	 * {@code rclpy.publisher.Publisher}.
	 */
	static final String PUBLISHER = "rclpy.publisher.Publisher";

	/**
	 * {@code rclpy.service.Service}.
	 */
	static final String SERVICE = "rclpy.service.Service";

	private RosTypes() {
	}

	/**
	 * Yields the type of an rclpy class.
	 *
	 * @param qualifiedName the qualified name of the class
	 *
	 * @return the type, or empty if the class is not part of the analysed
	 *             program
	 */
	static Optional<PyClassType> lookup(
			String qualifiedName) {
		return PyClassType.isRegistered(qualifiedName)
				? Optional.of(PyClassType.lookup(qualifiedName))
				: Optional.empty();
	}
}
