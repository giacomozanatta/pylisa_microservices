package it.unive.pylisa.interprocedural;

import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.type.Type;
import it.unive.pylisa.cfg.type.PyFunctionType;
import java.util.function.Function;

/**
 * Bridge between the Python type system and the network-aware analysis: maps a
 * {@link PyFunctionType} found in the abstract state (e.g. the type of a value
 * passed as an HTTP route handler) to its full name + source location, in the
 * format consumed by
 * {@code it.unive.lisa.analysis.network.NetworkAwareAbstractDomain}.
 * <p>
 * This is a per-frontend adapter; a hypothetical Go frontend would supply its
 * own analogue (e.g. {@code GoHandlerExtractor}). After the inversion of the
 * pylisa &harr; lisa-network dependency, this class will move into lisa-network
 * as {@code it.unive.lisa.analysis.network.adapters.python.PythonAdapter}.
 */
public final class PyHandlerExtractor {

	private PyHandlerExtractor() {
	}

	/**
	 * Convenience factory: builds the extractor lambda that resolves a
	 * {@link PyFunctionType} to its handler's full name. Used in
	 * {@code NetworkAwareAbstractDomain} when constructing the domain instance.
	 *
	 * @return the extractor lambda
	 */
	public static Function<Type, String> handlerNameExtractor() {
		return t -> {
			if (t instanceof PyFunctionType pft) {
				CodeMember cm = pft.getUnit().getFunction();
				if (cm != null) {
					// Use the function-unit name (e.g.
					// "routers.tiles.delete_tile_cache")
					// rather than the CFG descriptor full name which appends
					// "::$call".
					String name = pft.getUnit().getName();
					String loc = cm.getDescriptor().getLocation().getCodeLocation();
					return name + " @ " + loc;
				}
			}
			return null;
		};
	}
}
