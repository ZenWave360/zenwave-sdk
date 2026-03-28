package io.zenwave360.sdk.plugins.support;

import java.util.Map;

public final class MethodBodyPlan {

    private final MethodBodyCase bodyCase;
    private final Map<String, Object> entity;
    private final boolean requiresMapping;
    private final boolean lifecycleManaged;
    private final boolean transitionDriven;

    public MethodBodyPlan(MethodBodyCase bodyCase,
                          Map<String, Object> entity,
                          boolean requiresMapping,
                          boolean lifecycleManaged,
                          boolean transitionDriven) {
        this.bodyCase = bodyCase;
        this.entity = entity;
        this.requiresMapping = requiresMapping;
        this.lifecycleManaged = lifecycleManaged;
        this.transitionDriven = transitionDriven;
    }

    public MethodBodyCase bodyCase() {
        return bodyCase;
    }

    public Map<String, Object> entity() {
        return entity;
    }

    public boolean requiresMapping() {
        return requiresMapping;
    }

    public boolean lifecycleManaged() {
        return lifecycleManaged;
    }

    public boolean transitionDriven() {
        return transitionDriven;
    }
}
