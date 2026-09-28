package it.unive.pylisa.ros2;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.FunctionalLattice;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A read-only view of one analysis state at one program point, answering the
 * questions a test asks about it: which variables exist, which heap objects a
 * reference denotes, which value an expression has, which errors may have been
 * raised.
 * <p>
 * Every question is answered through the analysis itself (heap rewriting and
 * type inference of the configured domains), so that references and fields are
 * interpreted exactly as the analysed program interprets them.
 * </p>
 *
 * @param <A> the kind of abstract state
 * @param <D> the kind of abstract domain
 */
final class StateView<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

	private final Analysis<A, D> analysis;

	private final AnalysisState<A> state;

	private final Statement point;

	private final ValueReader reader;

	/**
	 * Builds the view.
	 *
	 * @param analysis the analysis that computed the state
	 * @param state    the state
	 * @param point    the program point the state refers to
	 * @param reader   the reader for the configured value domain
	 */
	StateView(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			Statement point,
			ValueReader reader) {
		this.analysis = analysis;
		this.state = state;
		this.point = point;
		this.reader = reader;
	}

	/**
	 * Yields the program point this view refers to.
	 *
	 * @return the statement
	 */
	Statement point() {
		return point;
	}

	/**
	 * Yields whether some execution reaches this point normally, that is,
	 * without an error being raised.
	 *
	 * @return {@code true} if the normal execution state is not bottom
	 */
	boolean isReachable() {
		return !state.getExecution().isBottom() && !state.getExecutionState().isBottom();
	}

	/**
	 * Yields the names of the types of the errors that may have been raised by
	 * the time this point is reached.
	 *
	 * @return the error type names, sorted
	 */
	Set<String> errors() {
		Set<String> names = new TreeSet<>();
		Set<AnalysisState.Error> raised = state.getErrors().getKeys();
		if (raised != null)
			raised.forEach(error -> names.add(error.getType().toString()));
		Set<Type> smashed = state.getSmashedErrors().getKeys();
		if (smashed != null)
			smashed.forEach(type -> names.add(type.toString()));
		return names;
	}

	/**
	 * Finds the program variable with the given name: a local variable of the
	 * function this point belongs to, or else a global of the {@code __main__}
	 * module, or else the only global of any module with that name.
	 *
	 * @param name the variable name, as written in the Python source
	 *
	 * @return the variable, if it exists in this state
	 */
	Optional<Identifier> variable(
			String name) {
		List<Identifier> inScope = inScopeIdentifiers();
		Optional<Identifier> local = inScope.stream().filter(id -> id.getName().equals(name)).findFirst();
		if (local.isPresent())
			return local;
		Optional<Identifier> main = inScope.stream()
				.filter(id -> id.getName().equals("$__main__::" + name))
				.findFirst();
		if (main.isPresent())
			return main;
		List<Identifier> globals = inScope.stream()
				.filter(id -> id.getName().startsWith("$") && id.getName().endsWith("::" + name))
				.collect(Collectors.toList());
		return globals.size() == 1 ? Optional.of(globals.get(0)) : Optional.empty();
	}

	/**
	 * Yields the heap locations of the objects a reference may point to.
	 *
	 * @param reference an expression denoting a reference
	 *
	 * @return the heap locations
	 */
	Set<HeapLocation> objectsPointedBy(
			SymbolicExpression reference) {
		return rewrite(new HeapDereference(Untyped.INSTANCE, reference, SyntheticLocation.INSTANCE)).stream()
				.filter(HeapLocation.class::isInstance)
				.map(HeapLocation.class::cast)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Yields the value of an expression, joined over everything the expression
	 * may denote.
	 *
	 * @param expression the expression
	 *
	 * @return the value, or empty if no execution gives the expression a value
	 */
	Optional<Val> valueOf(
			SymbolicExpression expression) {
		Val result = null;
		for (SymbolicExpression denoted : rewrite(expression)) {
			Val value;
			if (denoted instanceof Identifier identifier)
				value = reader.read(stateComponents().valueState, identifier);
			else if (denoted instanceof Constant constant)
				value = Val.exact(constant.getValue());
			else
				value = Val.top();
			if (value != null)
				result = result == null ? value : result.join(value);
		}
		return Optional.ofNullable(result);
	}

	/**
	 * Yields the runtime types an expression may have.
	 *
	 * @param expression the expression
	 *
	 * @return the types
	 */
	Set<Type> typesOf(
			SymbolicExpression expression) {
		try {
			return analysis.getRuntimeTypesOf(state, expression, point);
		} catch (SemanticException e) {
			throw new IllegalStateException("Cannot type " + expression + " at " + point.getLocation(), e);
		}
	}

	/**
	 * Builds the expression that accesses a field of the object a reference
	 * points to, as the Python attribute access {@code reference.name} does.
	 *
	 * @param reference an expression denoting a reference
	 * @param name      the field name
	 *
	 * @return the field access
	 */
	static SymbolicExpression field(
			SymbolicExpression reference,
			String name) {
		HeapDereference container = new HeapDereference(Untyped.INSTANCE, reference, SyntheticLocation.INSTANCE);
		Variable child = new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
		return new AccessChild(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
	}

	private Set<SymbolicExpression> rewrite(
			SymbolicExpression expression) {
		try {
			Set<SymbolicExpression> rewritten = new LinkedHashSet<>();
			analysis.rewrite(state, expression, point).forEach(rewritten::add);
			return rewritten;
		} catch (SemanticException e) {
			throw new IllegalStateException("Cannot rewrite " + expression + " at " + point.getLocation(), e);
		}
	}

	private List<Identifier> inScopeIdentifiers() {
		if (!(stateComponents().typeState instanceof FunctionalLattice<?, ?, ?> types) || types.getKeys() == null)
			return List.of();
		return types.getKeys().stream()
				.filter(Identifier.class::isInstance)
				.map(Identifier.class::cast)
				.filter(id -> !(id instanceof OutOfScopeIdentifier))
				.collect(Collectors.toList());
	}

	private SimpleAbstractState<?, ?, ?> stateComponents() {
		if (state.getExecutionState() instanceof SimpleAbstractState<?, ?, ?> components)
			return components;
		throw new IllegalStateException("Only states made of heap, value and type components are supported, got "
				+ state.getExecutionState().getClass().getName());
	}
}
