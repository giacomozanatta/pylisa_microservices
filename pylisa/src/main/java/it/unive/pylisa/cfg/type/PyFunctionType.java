package it.unive.pylisa.cfg.type;

import it.unive.lisa.type.*;

import java.util.Collections;
import java.util.Set;

public class PyFunctionType implements FunctionType {

    /**
     * The singleton instance of this class.
     */
    public static final PyFunctionType INSTANCE = new PyFunctionType();

    /**
     * Builds the type. This constructor is visible to allow subclassing:
     * instances of this class should be unique, and the singleton can be
     * retrieved through field {@link #INSTANCE}.
     */
    protected PyFunctionType() {
    }
    @Override
    public boolean canBeAssignedTo(
            Type other) {
        return other.isFunctionType() || other.isUntyped();
    }

    @Override
    public Type commonSupertype(
            Type other) {
        return other.isFunctionType() ? this : Untyped.INSTANCE;
    }

    @Override
    public String toString() {
        return "function";
    }

    @Override
    public boolean equals(
            Object other) {
        return other instanceof BooleanType;
    }

    @Override
    public int hashCode() {
        return BooleanType.class.getName().hashCode();
    }

    @Override
    public Set<Type> allInstances(
            TypeSystem types) {
        return Collections.singleton(this);
    }


}
