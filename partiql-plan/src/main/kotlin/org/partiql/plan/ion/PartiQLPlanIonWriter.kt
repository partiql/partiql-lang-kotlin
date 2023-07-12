package org.partiql.plan.ion

import com.amazon.ionelement.api.IonElement
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PartiQLVersion
import org.partiql.plan.ion.impl.PartiQLPlanIonWriter_VERSION_0_1

/**
 * Transforms a PartiQL Plan into its canonical Ion representation.
 */
public interface PartiQLPlanIonWriter {

    @Throws(PlanWriterException::class)
    public fun toIon(plan: PartiQLPlan): IonElement

    @Throws(PlanWriterException::class)
    public fun toIonDebug(plan: PartiQLPlan): IonElement

    public companion object {

        public fun get(version: PartiQLVersion): PartiQLPlanIonWriter = when (version) {
            PartiQLVersion.VERSION_0_0 -> error("Does not exist")
            PartiQLVersion.VERSION_0_1 -> PartiQLPlanIonWriter_VERSION_0_1
        }
    }
}
