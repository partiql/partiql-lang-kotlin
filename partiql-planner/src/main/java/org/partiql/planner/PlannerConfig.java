package org.partiql.planner;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * Represents the configuration for the {@link PartiQLPlanner}.
 *
 * @see PartiQLPlanner
 * @see PlannerConfigBuilder
 */
public interface PlannerConfig {

    /**
     * @return the {@link PErrorListener} that the {@link PartiQLPlanner} may use to emit errors/warnings.
     */
    @NotNull
    PErrorListener getErrorListener();
}
