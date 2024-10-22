package org.partiql.planner;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * Represents the configuration for the {@link PartiQLPlanner}.
 *
 * @see PartiQLPlanner
 */
@lombok.Builder(builderClassName = "Builder")
public final class PlannerContext {

    @lombok.NonNull
    @lombok.Builder.Default
    private final PErrorListener listener = PErrorListener.abortOnError();

    /**
     * @return the {@link PErrorListener} that the {@link PartiQLPlanner} may use to emit errors/warnings.
     */
    @NotNull
    public PErrorListener getErrorListener() {
        return listener;
    }
}
