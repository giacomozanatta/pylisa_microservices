package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;
import it.unive.pylisa.cfg.type.PyClassType;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates the objects that stand for the ROS 2 entities of a node (publishers,
 * services, ...), with the fields every reader of the analysis state relies
 * on. Every entity refers to the node that owns it through the field
 * {@value #NODE}.
 */
final class EntityModels {

	/**
	 * The field of an entity that refers to its node.
	 */
	static final String NODE = "$node";

	/**
	 * The field of a timer that tells whether it is canceled.
	 */
	static final String CANCELED = "$canceled";

	private static final Logger LOG = LogManager.getLogger(EntityModels.class);

	private EntityModels() {
	}

	/**
	 * Creates a publisher.
	 *
	 * @param <A>       the kind of abstract state
	 * @param <D>       the kind of abstract domain
	 * @param state     the state
	 * @param build     the factory of expressions
	 * @param site      the allocation site of the publisher
	 * @param node      a reference to the owning node
	 * @param msgType   the message type
	 * @param topic     the topic name as given by the program
	 * @param topicName  the resolved topic name
	 * @param qosProfile the quality of service given by the program
	 * @param qosDepth   the depth of the history kept by the publisher
	 *
	 * @return the state after the creation, whose computed value is the
	 *             reference to the publisher
	 *
	 * @throws SemanticException if the publisher cannot be created
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> publisher(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression msgType,
			SymbolicExpression topic,
			SymbolicExpression topicName,
			SymbolicExpression qosProfile,
			SymbolicExpression qosDepth)
			throws SemanticException {
		return create(state, RosTypes.PUBLISHER, site, (created, publisher) -> created
				.write(publisher, NODE, node)
				.write(publisher, "msg_type", msgType)
				.write(publisher, "topic", topic)
				.write(publisher, "topic_name", topicName)
				.write(publisher, "qos_profile", qosProfile)
				.write(publisher, "qos_depth", qosDepth)
				.returning(publisher));
	}

	/**
	 * Creates a subscription.
	 *
	 * @param <A>        the kind of abstract state
	 * @param <D>        the kind of abstract domain
	 * @param state      the state
	 * @param site       the allocation site of the subscription
	 * @param node       a reference to the owning node
	 * @param msgType    the message type
	 * @param topic      the topic name as given by the program
	 * @param topicName  the resolved topic name
	 * @param callback   the callback run on every message
	 * @param qosProfile the quality of service given by the program
	 * @param qosDepth   the depth of the history kept by the subscription
	 * @param raw        whether messages are delivered serialized
	 *
	 * @return the state after the creation, whose computed value is the
	 *             reference to the subscription
	 *
	 * @throws SemanticException if the subscription cannot be created
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> subscription(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression msgType,
			SymbolicExpression topic,
			SymbolicExpression topicName,
			SymbolicExpression callback,
			SymbolicExpression qosProfile,
			SymbolicExpression qosDepth,
			SymbolicExpression raw)
			throws SemanticException {
		return create(state, RosTypes.SUBSCRIPTION, site, (created, subscription) -> created
				.write(subscription, NODE, node)
				.write(subscription, "msg_type", msgType)
				.write(subscription, "topic", topic)
				.write(subscription, "topic_name", topicName)
				.write(subscription, "callback", callback)
				.write(subscription, "qos_profile", qosProfile)
				.write(subscription, "qos_depth", qosDepth)
				.write(subscription, "raw", raw)
				.returning(subscription));
	}

	/**
	 * Creates a timer, which is not canceled.
	 *
	 * @param <A>      the kind of abstract state
	 * @param <D>      the kind of abstract domain
	 * @param state    the state
	 * @param build    the factory of expressions
	 * @param site     the allocation site of the timer
	 * @param node     a reference to the owning node
	 * @param periodNs the period in nanoseconds
	 * @param callback the callback run at every period, or {@code None}
	 *
	 * @return the state after the creation, whose computed value is the
	 *             reference to the timer
	 *
	 * @throws SemanticException if the timer cannot be created
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> timer(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression periodNs,
			SymbolicExpression callback)
			throws SemanticException {
		return create(state, RosTypes.TIMER, site, (created, timer) -> created
				.write(timer, NODE, node)
				.write(timer, "timer_period_ns", periodNs)
				.write(timer, "callback", callback)
				.write(timer, CANCELED, build.bool(false))
				.returning(timer));
	}

	/**
	 * Creates a client of a service.
	 *
	 * @param <A>         the kind of abstract state
	 * @param <D>         the kind of abstract domain
	 * @param state       the state
	 * @param site        the allocation site of the client
	 * @param node        a reference to the owning node
	 * @param srvType     the service type
	 * @param srvName     the service name as given
	 * @param serviceName the resolved service name
	 * @param qosProfile  the quality of service
	 *
	 * @return the state after the creation, whose computed value is the
	 *             reference to the client
	 *
	 * @throws SemanticException if the client cannot be created
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> client(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression srvType,
			SymbolicExpression srvName,
			SymbolicExpression serviceName,
			SymbolicExpression qosProfile)
			throws SemanticException {
		return create(state, RosTypes.CLIENT, site, (created, client) -> created
				.write(client, NODE, node)
				.write(client, "srv_type", srvType)
				.write(client, "srv_name", srvName)
				.write(client, "service_name", serviceName)
				.write(client, "qos_profile", qosProfile)
				.returning(client));
	}

	/**
	 * Creates a guard condition.
	 *
	 * @param <A>      the kind of abstract state
	 * @param <D>      the kind of abstract domain
	 * @param state    the state
	 * @param site     the allocation site of the guard condition
	 * @param node     a reference to the owning node
	 * @param callback the callback run when the condition is triggered
	 *
	 * @return the state after the creation, whose computed value is the
	 *             reference to the guard condition
	 *
	 * @throws SemanticException if the guard condition cannot be created
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> guardCondition(
			ModelState<A, D> state,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression callback)
			throws SemanticException {
		return create(state, RosTypes.GUARD_CONDITION, site, (created, guard) -> created
				.write(guard, NODE, node)
				.write(guard, "callback", callback)
				.returning(guard));
	}

	/**
	 * Creates a service.
	 *
	 * @param <A>         the kind of abstract state
	 * @param <D>         the kind of abstract domain
	 * @param state       the state
	 * @param build       the factory of expressions
	 * @param site        the allocation site of the service
	 * @param node        a reference to the owning node
	 * @param srvType     the service type
	 * @param srvName     the service name as given
	 * @param serviceName the resolved service name
	 * @param callback    the callback handling requests
	 * @param qosProfile  the quality of service
	 *
	 * @return the state after the creation, whose computed value is the
	 *             reference to the service
	 *
	 * @throws SemanticException if the service cannot be created
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> service(
			ModelState<A, D> state,
			Expressions build,
			CodeLocation site,
			SymbolicExpression node,
			SymbolicExpression srvType,
			SymbolicExpression srvName,
			SymbolicExpression serviceName,
			SymbolicExpression callback,
			SymbolicExpression qosProfile)
			throws SemanticException {
		return create(state, RosTypes.SERVICE, site, (created, service) -> created
				.write(service, NODE, node)
				.write(service, "srv_type", srvType)
				.write(service, "srv_name", srvName)
				.write(service, "service_name", serviceName)
				.write(service, "callback", callback)
				.write(service, "qos_profile", qosProfile)
				.returning(service));
	}

	/**
	 * Allocates an object of an rclpy class and initializes it. When the class
	 * is not part of the analysed program, no object can be created: the
	 * outcome is an unknown value.
	 *
	 * @param <A>        the kind of abstract state
	 * @param <D>        the kind of abstract domain
	 * @param state      the state
	 * @param className  the qualified name of the class
	 * @param site       the allocation site
	 * @param initialize the initialization of each allocated object
	 *
	 * @return the join of the initialized states
	 *
	 * @throws SemanticException if the object cannot be created
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> create(
			ModelState<A, D> state,
			String className,
			CodeLocation site,
			ModelState.Step<A, D, SymbolicExpression> initialize)
			throws SemanticException {
		Optional<PyClassType> type = RosTypes.lookup(className);
		if (type.isEmpty()) {
			LOG.warn("{} is not part of the analysed program: no object created at {}", className, site);
			return state.returning(new PushAny(Untyped.INSTANCE, site));
		}
		ModelState<A, D> allocated = state.allocate(type.get(), site);
		return allocated.forEach(allocated.values(), initialize);
	}
}
