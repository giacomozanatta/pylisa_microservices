package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.pylisa.cfg.type.PyExceptionType;

/**
 * The rules ROS 2 applies to the names of nodes and namespaces, as rcl and rmw
 * apply them when a node is created.
 * <p>
 * Every rule is a case analysis on abstract strings: each case is explored
 * under the assumption that its condition holds, a case that no execution can
 * take is skipped, and when the domains cannot decide a condition both cases
 * are explored. The outcome is therefore correct for any configured string
 * domain, and as precise as that domain.
 * </p>
 */
final class RosNames {

	/**
	 * A valid node name: letters, digits and underscores, not starting with a
	 * digit.
	 */
	static final String NODE_NAME = "[A-Za-z_][A-Za-z0-9_]*";

	/**
	 * A valid, normalized namespace: either {@code /} or a sequence of
	 * {@code /token} where every token is a valid node name. This excludes a
	 * trailing {@code /}, empty tokens ({@code //}) and tokens starting with a
	 * digit.
	 */
	static final String NAMESPACE = "/|(/[A-Za-z_][A-Za-z0-9_]*)+";

	private RosNames() {
	}

	/**
	 * Normalizes a node namespace as rclpy and rcl do: {@code None} and the
	 * empty string become {@code /}, and a namespace not starting with
	 * {@code /} gets one prepended.
	 *
	 * @param <A>        the kind of abstract state
	 * @param <D>        the kind of abstract domain
	 * @param state      the state
	 * @param build      the factory of expressions
	 * @param namespace  the namespace given by the program
	 * @param normalized what to do with the normalized namespace
	 *
	 * @return the join of the outcomes of every case
	 *
	 * @throws SemanticException if a condition cannot be evaluated
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> normalizeNamespace(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression namespace,
			ModelState.Step<A, D, SymbolicExpression> normalized)
			throws SemanticException {
		return state.branch(build.or(build.isNone(namespace), build.equal(namespace, build.string(""))),
				(root, condition) -> normalized.apply(root, build.string("/")),
				(given, condition) -> given.branch(build.startsWith(namespace, "/"),
						(absolute, c) -> normalized.apply(absolute, namespace),
						(relative, c) -> normalized.apply(relative, build.concat(build.string("/"), namespace))));
	}

	/**
	 * Continues with a name only where it is valid, and raises the given
	 * exception where it is not.
	 *
	 * @param <A>     the kind of abstract state
	 * @param <D>     the kind of abstract domain
	 * @param state   the state
	 * @param build   the factory of expressions
	 * @param name    the name
	 * @param pattern the regular expression every valid name matches entirely
	 * @param error   the exception raised for an invalid name
	 * @param valid   what to do with a valid name
	 *
	 * @return the join of the outcomes
	 *
	 * @throws SemanticException if the validity cannot be evaluated
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> requireValid(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression name,
			String pattern,
			PyExceptionType error,
			ModelState.Step<A, D, SymbolicExpression> valid)
			throws SemanticException {
		return state.branch(build.matches(name, pattern),
				(accepted, condition) -> valid.apply(accepted, name),
				(rejected, condition) -> rejected.raise(error));
	}

	/**
	 * Computes the fully qualified name of a node, as rcl does once when the
	 * node is created: the namespace followed by the name, separated by a
	 * {@code /} unless the namespace is the root one.
	 *
	 * @param <A>       the kind of abstract state
	 * @param <D>       the kind of abstract domain
	 * @param state     the state
	 * @param build     the factory of expressions
	 * @param namespace the normalized namespace
	 * @param name      the node name
	 * @param qualified what to do with the fully qualified name
	 *
	 * @return the join of the outcomes
	 *
	 * @throws SemanticException if a condition cannot be evaluated
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> fullyQualifiedName(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression namespace,
			SymbolicExpression name,
			ModelState.Step<A, D, SymbolicExpression> qualified)
			throws SemanticException {
		return state.branch(build.equal(namespace, build.string("/")),
				(root, condition) -> qualified.apply(root, build.concat(namespace, name)),
				(nested, condition) -> qualified.apply(nested, build.concat(namespace, build.string("/"), name)));
	}
}
