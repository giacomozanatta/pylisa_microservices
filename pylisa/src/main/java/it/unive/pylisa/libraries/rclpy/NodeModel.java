package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;
import java.util.List;

/**
 * What constructing an rclpy node does to the program state.
 * <p>
 * The name, namespace and fully qualified name of a node live in the C layer
 * of ROS 2 and are computed once, when the node is created; they are stored in
 * the fields {@value #NAME}, {@value #NAMESPACE} and {@value #FULLY_QUALIFIED}
 * of the node object, which the accessors of the node read. Every node also
 * owns a publisher on {@code /parameter_events} and, unless disabled, the six
 * services that expose its parameters.
 * </p>
 */
final class NodeModel {

	/**
	 * The field holding the node name.
	 */
	static final String NAME = "$name";

	/**
	 * The field holding the normalized namespace.
	 */
	static final String NAMESPACE = "$namespace";

	/**
	 * The field holding the fully qualified name.
	 */
	static final String FULLY_QUALIFIED = "$fqn";

	/**
	 * The field holding the context the node belongs to.
	 */
	static final String CONTEXT = "$context";

	/**
	 * The name of the variable holding rclpy's default context, which
	 * {@code rclpy.init()} initializes.
	 */
	static final String DEFAULT_CONTEXT = "$rclpy.utilities::g_default_context";

	/**
	 * The field of a context that tells whether it is initialized and not shut
	 * down.
	 */
	static final String CONTEXT_OK = "ok";

	/**
	 * The history depth of the publisher on {@code /parameter_events}.
	 */
	private static final int PARAMETER_EVENTS_DEPTH = 1000;

	/**
	 * The parameter services of a node: the suffix of their name, and their
	 * type.
	 */
	private static final List<String[]> PARAMETER_SERVICES = List.of(
			new String[] { "describe_parameters", "rcl_interfaces.srv.DescribeParameters" },
			new String[] { "get_parameters", "rcl_interfaces.srv.GetParameters" },
			new String[] { "get_parameter_types", "rcl_interfaces.srv.GetParameterTypes" },
			new String[] { "list_parameters", "rcl_interfaces.srv.ListParameters" },
			new String[] { "set_parameters", "rcl_interfaces.srv.SetParameters" },
			new String[] { "set_parameters_atomically", "rcl_interfaces.srv.SetParametersAtomically" });

	private NodeModel() {
	}

	/**
	 * Initializes a node object as {@code Node.__init__} does.
	 *
	 * @param <A>                    the kind of abstract state
	 * @param <D>                    the kind of abstract domain
	 * @param state                  the state before the initialization
	 * @param site                   the location of the call that creates the
	 *                                   node
	 * @param self                   a reference to the node object
	 * @param name                   the node name
	 * @param namespace              the namespace given by the program
	 * @param context                the context given by the program, or
	 *                                   {@code None}
	 * @param startParameterServices whether the parameter services are created
	 *
	 * @return the state after the initialization, with {@code None} as
	 *             computed value
	 *
	 * @throws SemanticException if the initialization cannot be computed
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> initialize(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression self,
			SymbolicExpression name,
			SymbolicExpression namespace,
			SymbolicExpression context,
			SymbolicExpression startParameterServices)
			throws SemanticException {
		return new Initialization<A, D>(new Expressions(site), site, self, startParameterServices)
				.run(state, name, namespace, context);
	}

	/**
	 * The steps of the initialization of one node, in the order rclpy and rcl
	 * perform them: check the context, validate the name, normalize and
	 * validate the namespace, compute the fully qualified name, then store the
	 * names and create the entities every node owns. Each step continues only
	 * with the executions that pass it.
	 *
	 * @param <A> the kind of abstract state
	 * @param <D> the kind of abstract domain
	 */
	private static final class Initialization<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

		private final Expressions build;

		private final CodeLocation site;

		private final SymbolicExpression self;

		private final SymbolicExpression startParameterServices;

		private Initialization(
				Expressions build,
				CodeLocation site,
				SymbolicExpression self,
				SymbolicExpression startParameterServices) {
			this.build = build;
			this.site = site;
			this.self = self;
			this.startParameterServices = startParameterServices;
		}

		private ModelState<A, D> run(
				ModelState<A, D> state,
				SymbolicExpression name,
				SymbolicExpression namespace,
				SymbolicExpression context)
				throws SemanticException {
			return requireInitialized(state, build, context,
					(initialized, usedContext) -> withContext(initialized, usedContext, name, namespace));
		}

		private ModelState<A, D> withContext(
				ModelState<A, D> state,
				SymbolicExpression context,
				SymbolicExpression name,
				SymbolicExpression namespace)
				throws SemanticException {
			return RosNames.requireValid(state, build, name, RosNames.NODE_NAME, RclpyExceptions.INVALID_NODE_NAME,
					(named, validName) -> withName(named, context, validName, namespace));
		}

		private ModelState<A, D> withName(
				ModelState<A, D> state,
				SymbolicExpression context,
				SymbolicExpression name,
				SymbolicExpression namespace)
				throws SemanticException {
			return RosNames.normalizeNamespace(state, build, namespace,
					(normalized, fullNamespace) -> RosNames.requireValid(normalized, build, fullNamespace,
							RosNames.NAMESPACE, RclpyExceptions.INVALID_NAMESPACE,
							(placed, validNamespace) -> withNamespace(placed, context, name, validNamespace)));
		}

		private ModelState<A, D> withNamespace(
				ModelState<A, D> state,
				SymbolicExpression context,
				SymbolicExpression name,
				SymbolicExpression namespace)
				throws SemanticException {
			return RosNames.fullyQualifiedName(state, build, namespace, name,
					(qualified, fullName) -> store(qualified, context, name, namespace, fullName));
		}

		private ModelState<A, D> store(
				ModelState<A, D> state,
				SymbolicExpression context,
				SymbolicExpression name,
				SymbolicExpression namespace,
				SymbolicExpression fullName)
				throws SemanticException {
			ModelState<A, D> named = state
					.write(self, NAME, name)
					.write(self, NAMESPACE, namespace)
					.write(self, FULLY_QUALIFIED, fullName)
					.write(self, CONTEXT, context)
					.write(self, EntityModels.DESTROYED, build.bool(false));
			return createBuiltInEntities(named, build, site, self, name, startParameterServices)
					.returning(build.none());
		}
	}

	/**
	 * Continues only with the executions in which the context of the node is
	 * initialized: rclpy raises {@code NotInitializedException} for a node
	 * created before {@code rclpy.init()} or after {@code rclpy.shutdown()}.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> requireInitialized(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression context,
			ModelState.Step<A, D, SymbolicExpression> initialized)
			throws SemanticException {
		SymbolicExpression defaultContext = defaultContext(build);
		return state.ifNone(context,
				(implicit, condition) -> requireOk(implicit, defaultContext, initialized),
				(explicit, condition) -> requireOk(explicit, context, initialized));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> requireOk(
			ModelState<A, D> state,
			SymbolicExpression context,
			ModelState.Step<A, D, SymbolicExpression> initialized)
			throws SemanticException {
		ModelState<A, D> ok = state.read(context, CONTEXT_OK);
		return state.forEach(ok.values(), (current, flag) -> current.branch(flag,
				(usable, condition) -> initialized.apply(usable, context),
				(unusable, condition) -> unusable.raise(RclpyExceptions.NOT_INITIALIZED)));
	}

	/**
	 * Continues where a node is not destroyed, and raises
	 * {@code InvalidHandle} where it may be: the methods of a node that use
	 * its C handle fail once {@code destroy_node()} freed it.
	 *
	 * @param <A>   the kind of abstract state
	 * @param <D>   the kind of abstract domain
	 * @param state the state
	 * @param node  a reference to the node
	 * @param alive what to do where the node may be used
	 *
	 * @return the join of the outcomes
	 *
	 * @throws SemanticException if the field cannot be read
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> requireAlive(
			ModelState<A, D> state,
			SymbolicExpression node,
			ModelState.Step<A, D, SymbolicExpression> alive)
			throws SemanticException {
		ModelState<A, D> destroyed = state.read(node, EntityModels.DESTROYED);
		return state.forEach(destroyed.values(), (current, flag) -> current.branch(flag,
				(gone, c) -> gone.raise(RclpyExceptions.INVALID_HANDLE),
				(present, c) -> alive.apply(present, node)));
	}

	/**
	 * Yields the variable holding rclpy's default context.
	 *
	 * @param build the factory of expressions, which provides the location
	 *
	 * @return the variable
	 */
	static GlobalVariable defaultContext(
			Expressions build) {
		return new GlobalVariable(Untyped.INSTANCE, DEFAULT_CONTEXT, build.location());
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> createBuiltInEntities(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression self,
			SymbolicExpression name,
			SymbolicExpression startParameterServices)
			throws SemanticException {
		ModelState<A, D> withEvents = EntityModels.publisher(state, build,
				new TaggedLocation(site, "parameter_events"), self,
				build.string("rcl_interfaces.msg.ParameterEvent"),
				build.string("/parameter_events"), build.string("/parameter_events"),
				build.unknown(), build.integer(PARAMETER_EVENTS_DEPTH));
		return withEvents.branch(startParameterServices,
				(enabled, condition) -> {
					ModelState<A, D> result = enabled;
					for (String[] service : PARAMETER_SERVICES) {
						// rclpy creates each service with the name
						// <node name>/<suffix>, resolved as any other service
						// name; the callback and the quality of service are
						// internal to rclpy
						SymbolicExpression srvName = build.concat(name, build.string("/" + service[0]));
						CodeLocation serviceSite = new TaggedLocation(site, service[0]);
						result = RosNames.resolve(result, build, self, srvName, RclpyExceptions.INVALID_SERVICE_NAME,
								(resolved, serviceName) -> EntityModels.service(resolved, build, serviceSite, self,
										build.string(service[1]), srvName, serviceName, build.unknown(),
										build.unknown()));
					}
					return result;
				},
				(disabled, condition) -> disabled);
	}
}
