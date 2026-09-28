package it.unive.pylisa.libraries.rclpy;

import it.unive.pylisa.cfg.type.PyExceptionType;

/**
 * The exception classes of {@code rclpy.exceptions} that the rclpy models
 * raise, with their superclasses as in rclpy.
 */
public final class RclpyExceptions {

	/**
	 * Raised when a node is created before {@code rclpy.init()} or after
	 * {@code rclpy.shutdown()}.
	 */
	public static final PyExceptionType NOT_INITIALIZED = PyExceptionType
			.subclass("rclpy.exceptions.NotInitializedException", PyExceptionType.EXCEPTION);

	/**
	 * The superclass of the exceptions raised by invalid ROS 2 names.
	 */
	public static final PyExceptionType NAME_VALIDATION = PyExceptionType
			.subclass("rclpy.exceptions.NameValidationException", PyExceptionType.EXCEPTION);

	/**
	 * Raised when a node namespace is invalid.
	 */
	public static final PyExceptionType INVALID_NAMESPACE = PyExceptionType
			.subclass("rclpy.exceptions.InvalidNamespaceException", NAME_VALIDATION);

	/**
	 * Raised when a node name is invalid.
	 */
	public static final PyExceptionType INVALID_NODE_NAME = PyExceptionType
			.subclass("rclpy.exceptions.InvalidNodeNameException", NAME_VALIDATION);

	/**
	 * Raised when a topic name is invalid.
	 */
	public static final PyExceptionType INVALID_TOPIC_NAME = PyExceptionType
			.subclass("rclpy.exceptions.InvalidTopicNameException", NAME_VALIDATION);

	/**
	 * Raised when a service name is invalid.
	 */
	public static final PyExceptionType INVALID_SERVICE_NAME = PyExceptionType
			.subclass("rclpy.exceptions.InvalidServiceNameException", NAME_VALIDATION);

	/**
	 * The superclass of the exceptions raised by parameter operations.
	 */
	public static final PyExceptionType PARAMETER = PyExceptionType
			.subclass("rclpy.exceptions.ParameterException", PyExceptionType.EXCEPTION);

	/**
	 * Raised when an undeclared parameter is read.
	 */
	public static final PyExceptionType PARAMETER_NOT_DECLARED = PyExceptionType
			.subclass("rclpy.exceptions.ParameterNotDeclaredException", PARAMETER);

	/**
	 * Raised when a parameter is declared twice.
	 */
	public static final PyExceptionType PARAMETER_ALREADY_DECLARED = PyExceptionType
			.subclass("rclpy.exceptions.ParameterAlreadyDeclaredException", PARAMETER);

	/**
	 * Raised when a parameter gets a value of a type other than the one it was
	 * declared with.
	 */
	public static final PyExceptionType INVALID_PARAMETER_TYPE = PyExceptionType
			.subclass("rclpy.exceptions.InvalidParameterTypeException", PARAMETER);

	/**
	 * Raised when a read-only parameter is changed or undeclared.
	 */
	public static final PyExceptionType PARAMETER_IMMUTABLE = PyExceptionType
			.subclass("rclpy.exceptions.ParameterImmutableException", PARAMETER);

	/**
	 * Raised when a parameter declared with only its type is read before it
	 * gets a value.
	 */
	public static final PyExceptionType PARAMETER_UNINITIALIZED = PyExceptionType
			.subclass("rclpy.exceptions.ParameterUninitializedException", PARAMETER);

	/**
	 * Raised when a destroyed entity, such as a destroyed node, is used.
	 */
	public static final PyExceptionType INVALID_HANDLE = PyExceptionType
			.subclass("rclpy._rclpy_pybind11.InvalidHandle", PyExceptionType.EXCEPTION);

	/**
	 * Raised when the C layer of rclpy reports an error, such as a timer with
	 * a negative period.
	 */
	public static final PyExceptionType RCL_ERROR = PyExceptionType
			.subclass("rclpy._rclpy_pybind11.RCLError", PyExceptionType.RUNTIME_ERROR);

	private RclpyExceptions() {
	}
}
