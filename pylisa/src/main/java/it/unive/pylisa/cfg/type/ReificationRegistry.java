package it.unive.pylisa.cfg.type;

import it.unive.lisa.type.Type;

import java.util.HashMap;
import java.util.Map;

public class ReificationRegistry {

    private static final Map<Type, ReifiedRoleType> rules = new HashMap<>();

    private ReificationRegistry() { }

    public static synchronized void registerRule(ReifiedRoleType rule) {
        rules.put(rule.getConcreteType(), rule);
    }

    /** Get a rule for a concrete type (creates one if missing) */
    public static synchronized ReifiedRoleType getReificationRule(Type concrete) {
        return rules.computeIfAbsent(concrete, t -> new ReifiedRoleType(t, t));
    }

    /** Reify a concrete type into its synthetic role type */
    public static Type reify(Type concrete) {
        return rules.get(concrete);
    }
}
