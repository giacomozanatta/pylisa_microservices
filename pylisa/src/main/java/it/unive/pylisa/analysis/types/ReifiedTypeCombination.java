package it.unive.pylisa.analysis.types;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.TypeCartesianCombination;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.type.TypeValue;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class ReifiedTypeCombination<L extends TypeValue<L>>
        extends TypeCartesianCombination<
                        ReifiedTypeCombination<L>,
                        TypeEnvironment<TypeSet>,
                        TypeEnvironment<L>
                        >
        implements BaseLattice<ReifiedTypeCombination<L>> {

    public ReifiedTypeCombination(TypeEnvironment<TypeSet> first, TypeEnvironment<L> second) {
        super(first, second);
    }

    @Override
    public ReifiedTypeCombination<L> lubAux(ReifiedTypeCombination<L> other) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqualAux(ReifiedTypeCombination<L> other) throws SemanticException {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public ReifiedTypeCombination<L> mk(TypeEnvironment<TypeSet> first, TypeEnvironment<L> second) {
        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public ReifiedTypeCombination<L> top() {
        return null;
    }

    @Override
    public ReifiedTypeCombination<L> bottom() {
        return null;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }

    @Override
    public ReifiedTypeCombination<L> store(Identifier target, Identifier source) throws SemanticException {
        return null;
    }
}
