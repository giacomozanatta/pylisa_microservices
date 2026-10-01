package it.unive.pylisa.analysis;

import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.CallGraphEdge;
import it.unive.lisa.interprocedural.callgraph.CallGraphNode;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.callgraph.events.CallResolved;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.pylisa.cfg.statement.PyCall;
import it.unive.pylisa.cfg.statement.PyResolvedCall;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The call graph of a Python program. A {@link PyCall} is resolved with
 * Python's dispatch, from the runtime types of its operands alone (see
 * {@link PyResolvedCall}); every other call is resolved as LiSA does.
 * <p>
 * Each resolution is computed once per call and array of types, and records
 * what LiSA's call graphs record: the call as a call site of each code member
 * it may reach (Python functions and library models alike), an edge from the
 * caller to each of them, and a {@link CallResolved} event. The calls that
 * apply a resolution's targets are internal to it: they are never call sites,
 * so a call is listed once whatever the number of contexts and fixpoint
 * iterations it is analysed in.
 * </p>
 * <p>
 * Resolutions are shared by equal calls, and the equality of calls ignores
 * whether a call has a receiver or applies a decorator: the frontend never
 * builds two equal calls that differ in either.
 * </p>
 */
public class PyCallGraph extends RTACallGraph {

	private Application app;

	private EventQueue events;

	private final Map<PyCall, Map<List<Set<Type>>, PyResolvedCall>> resolutions = new HashMap<>();

	private final Map<CodeMember, Set<Call>> sites = new HashMap<>();

	/**
	 * The calls that apply the targets of the resolutions: LiSA registers the
	 * calls it analyses as call sites, but these belong to the Python call
	 * that is resolved, which is the call site.
	 */
	private final Set<Call> applications = Collections.newSetFromMap(new IdentityHashMap<>());

	@Override
	public void init(
			Application app,
			EventQueue events)
			throws CallGraphConstructionException {
		super.init(app, events);
		this.app = app;
		this.events = events;
		resolutions.clear();
		sites.clear();
		applications.clear();
	}

	@Override
	public void registerCall(
			CFGCall call) {
		if (!applications.contains(call))
			super.registerCall(call);
	}

	@Override
	public Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		if (!(call instanceof PyCall site))
			return super.resolve(call, types, aliasing);
		Map<List<Set<Type>>, PyResolvedCall> byTypes = resolutions.computeIfAbsent(site, c -> new HashMap<>());
		List<Set<Type>> key = Arrays.asList(types);
		PyResolvedCall known = byTypes.get(key);
		if (known != null)
			return known;
		PyResolvedCall resolved = new PyResolvedCall(site, types);
		resolved.setSource(call);
		byTypes.put(key, resolved);
		applications.addAll(resolved.applications());
		CallGraphNode caller = node(call.getCFG());
		for (CodeMember target : resolved.getTargets()) {
			addEdge(new CallGraphEdge(caller, node(target)));
			sites.computeIfAbsent(target, member -> new LinkedHashSet<>()).add(call);
		}
		if (events != null)
			events.post(new CallResolved(call, types, aliasing, resolved));
		return resolved;
	}

	private CallGraphNode node(
			CodeMember member) {
		CallGraphNode node = new CallGraphNode(this, member);
		if (!containsNode(node))
			addNode(node, app.getEntryPoints().contains(member));
		return node;
	}

	@Override
	public Collection<Call> getCallSites(
			CodeMember member) {
		Set<Call> python = sites.get(member);
		if (python == null)
			return super.getCallSites(member);
		Set<Call> all = new LinkedHashSet<>(super.getCallSites(member));
		all.addAll(python);
		return all;
	}
}
