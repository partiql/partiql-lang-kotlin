package org.partiql.planner;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

/**
 * Represents the configuration for the {@link PartiQLPlanner}.
 *
 * @see PartiQLPlanner
 * @see PlannerConfigBuilder
 */
public interface PlannerConfig {

    /**
     * @return the {@link ErrorListener} that the {@link PartiQLPlanner} may use to emit errors/warnings.
     */
    @NotNull
    ErrorListener getErrorListener();
}
