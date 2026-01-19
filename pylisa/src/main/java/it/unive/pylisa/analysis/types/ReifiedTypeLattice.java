package it.unive.pylisa.analysis.types;

import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collections;
import java.util.Map;

/**
 * Functional lattice mapping identifiers to TypeSet.
 */
public class ReifiedTypeLattice
        extends FunctionalLattice<ReifiedTypeLattice, Identifier, TypeSet> {

    /** Default constructor: empty store */
    public ReifiedTypeLattice() {
        super(new TypeSet()); // default is empty TypeSet
    }

    private ReifiedTypeLattice(TypeSet lattice, Map<Identifier, TypeSet> function) {
        super(lattice, function);
    }

    @Override
    public TypeSet stateOfUnknown(Identifier key) {
        return lattice;
    }

    @Override
    public ReifiedTypeLattice mk(TypeSet lattice, Map<Identifier, TypeSet> function) {
        return new ReifiedTypeLattice(lattice, function);
    }

    @Override
    public ReifiedTypeLattice top() {
        return null;
    }

    @Override
    public ReifiedTypeLattice bottom() {
        return null;
    }
}
