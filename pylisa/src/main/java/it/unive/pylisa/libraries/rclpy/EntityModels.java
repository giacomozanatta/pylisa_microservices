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
	 * @param topicName the resolved topic name
	 * @param qosDepth  the depth of the history kept by the publisher
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
			SymbolicExpression qosDepth)
			throws SemanticException {
		return create(state, RosTypes.PUBLISHER, site, (created, publisher) -> created
				.write(publisher, NODE, node)
				.write(publisher, "msg_type", msgType)
				.write(publisher, "topic", topic)
				.write(publisher, "topic_name", topicName)
				.write(publisher, "qos_depth", qosDepth)
				.returning(publisher));
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
			SymbolicExpression callback)
			throws SemanticException {
		return create(state, RosTypes.SERVICE, site, (created, service) -> created
				.write(service, NODE, node)
				.write(service, "srv_type", srvType)
				.write(service, "srv_name", srvName)
				.write(service, "service_name", serviceName)
				.write(service, "callback", callback)
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
