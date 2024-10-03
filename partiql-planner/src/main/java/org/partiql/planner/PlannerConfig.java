package org.partiql.planner;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

/**
 * Represents the configuration for the [PartiQLPlanner].
 *
 * @see PartiQLPlanner
 * @see PlannerConfigBuilder
 */
public interface PlannerConfig {

    /**
     * @return the [ErrorListener] that the [PartiQLPlanner] may use to emit errors/warnings.
     */
    @NotNull
    ErrorListener getErrorListener();
}
