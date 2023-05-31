package org.partiql.plan

import org.partiql.plan.builder.PlanFactoryImpl

/**
 * Singleton instance of the default factory. Also accessible via `PlanFactory.DEFAULT`.
 */
object Plan : PlanBaseFactory()

/**
 * PlanBaseFactory can be used to create a factory which extends from the factory provided by PlanFactory.DEFAULT.
 */
public abstract class PlanBaseFactory : PlanFactoryImpl() {
    // internal default overrides here
}
