package org.partiql.plan

import org.partiql.plan.builder.PlanFactoryImpl
import java.util.Random

/**
 * Singleton instance of the default factory. Also accessible via `PlanFactory.DEFAULT`.
 */
object Plan : PlanBaseFactory() {

    private val alphabet: Array<Char> = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toTypedArray()
    private val random = Random()

    /**
     * Stateless _id generation as this is a program-level singleton, eight random
     */
    override val _id: () -> String = {
        val buffer = CharArray(8) { alphabet[random.nextInt(alphabet.size)] }
        "plan-" + String(buffer)
    }
}

/**
 * PlanBaseFactory can be used to create a factory which extends from the factory provided by PlanFactory.DEFAULT.
 */
public abstract class PlanBaseFactory : PlanFactoryImpl() {
    // internal default overrides here
}
