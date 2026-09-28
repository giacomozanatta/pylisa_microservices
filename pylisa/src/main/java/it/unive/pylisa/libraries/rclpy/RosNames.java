package it.unive.pylisa.libraries.rclpy;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.program.type.StringType;
import it.unive.pylisa.cfg.type.PyExceptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The rules ROS 2 applies to the names of nodes, namespaces, topics and
 * services, as rcl and rmw apply them when a node or one of its entities is
 * created.
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

	/**
	 * A topic or service name as a program may give it, when it contains no
	 * substitution: {@code ~}, {@code ~} followed by one letter, digit or
	 * underscore (rcl checks that {@code ~} is followed by {@code /} only in
	 * names longer than two characters), or a sequence of {@code /}-separated
	 * tokens, each a valid node name, optionally preceded by {@code /}
	 * (absolute) or {@code ~/} (private). This accepts exactly the names that
	 * pass both the validation of the given name and the validation of its
	 * expansion, apart from the length limit, which only the expansion has.
	 */
	static final String TOPIC = "~[A-Za-z0-9_]?|(~/|/)?[A-Za-z_][A-Za-z0-9_]*(/[A-Za-z_][A-Za-z0-9_]*)*";

	/**
	 * A fully expanded topic or service name: absolute, made of valid tokens,
	 * at most 247 characters long.
	 */
	static final String FULL_TOPIC = "(?=.{1,247}$)(/[A-Za-z_][A-Za-z0-9_]*)+";

	private static final Logger LOG = LogManager.getLogger(RosNames.class);

	private RosNames() {
	}

	/**
	 * Resolves a topic or service name against a node, as rcl does when the
	 * entity is created: a name with a substitution (such as {@code {node}})
	 * is unknown, and may also be rejected as invalid or as an unknown
	 * substitution ({@code RCLError}); an invalid name raises; an absolute
	 * name stays as it is; a name starting with {@code ~} continues the fully
	 * qualified name of the node; any other name is relative to the namespace
	 * of the node. Remapping rules given from outside the program are not
	 * applied.
	 *
	 * @param <A>      the kind of abstract state
	 * @param <D>      the kind of abstract domain
	 * @param state    the state
	 * @param build    the factory of expressions
	 * @param node     a reference to the node
	 * @param name     the name as given by the program
	 * @param invalid  the exception raised for an invalid name
	 * @param resolved what to do with the resolved name
	 *
	 * @return the join of the outcomes of every case
	 *
	 * @throws SemanticException if a condition cannot be evaluated
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> resolve(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name,
			PyExceptionType invalid,
			ModelState.Step<A, D, SymbolicExpression> resolved)
			throws SemanticException {
		return state.branch(build.contains(name, "{"),
				(substituted, condition) -> {
					LOG.info("substitution-not-modelled: the name created at {} is unknown", build.location());
					return resolved.apply(substituted, build.unknown(StringType.INSTANCE))
							.lub(substituted.raise(invalid))
							.lub(substituted.raise(RclpyExceptions.RCL_ERROR));
				},
				(plain, condition) -> requireValid(plain, build, name, TOPIC, invalid,
						(valid, validName) -> expand(valid, build, node, validName,
								(expanded, fullName) -> requireValid(expanded, build, fullName, FULL_TOPIC,
										invalid, resolved))));
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> expand(
			ModelState<A, D> state,
			Expressions build,
			SymbolicExpression node,
			SymbolicExpression name,
			ModelState.Step<A, D, SymbolicExpression> expanded)
			throws SemanticException {
		return state.branch(build.startsWith(name, "/"),
				(absolute, c) -> expanded.apply(absolute, name),
				// rcl replaces the leading ~ with the fully qualified name
				(relative, c) -> relative.branch(build.startsWith(name, "~"),
						(priv, c1) -> withNodeField(priv, node, NodeModel.FULLY_QUALIFIED,
								(qualified, fqn) -> expanded.apply(qualified,
										build.concat(fqn, build.suffix(name, 1)))),
						(plain, c1) -> withNodeField(plain, node, NodeModel.NAMESPACE,
								(placed, namespace) -> placed.branch(build.equal(namespace, build.string("/")),
										(root, c2) -> expanded.apply(root, build.concat(build.string("/"), name)),
										(nested, c2) -> expanded.apply(nested,
												build.concat(namespace, build.string("/"), name))))));
	}

	/**
	 * Continues with every value of a field of the node.
	 */
	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> ModelState<A, D> withNodeField(
			ModelState<A, D> state,
			SymbolicExpression node,
			String field,
			ModelState.Step<A, D, SymbolicExpression> step)
			throws SemanticException {
		return state.forEach(state.read(node, field).values(), step);
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
