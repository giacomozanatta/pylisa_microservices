package it.unive.pylisa.cfg.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReifiedRoleType implements Type {


    private final Type concreteType;
    private final Type roleType;
    private final Map<String, String> parameterMappings = new HashMap<>();

    public ReifiedRoleType(Type concreteType, Type roleType) {
        this.concreteType = concreteType;
        this.roleType = roleType;
    }

    public Type getConcreteType() {
        return concreteType;
    }

    public Type getRoleType() {
        return roleType;
    }

    public boolean matches(Type t) {
        return t.equals(concreteType);
    }

    public void addParameterMapping(String concreteParam, String roleParam) {
        parameterMappings.put(concreteParam, roleParam);
    }

    public Map<String, String> getParameterMappings() {
        return Collections.unmodifiableMap(parameterMappings);
    }

    @Override
    public boolean canBeAssignedTo(Type other) {
        return false;
    }

    @Override
    public Type commonSupertype(Type other) {
        return null;
    }

    @Override
    public Set<Type> allInstances(TypeSystem types) {
        return Set.of();
    }
}
