package org.partiql.plan

/**
 * A [Plan] holds operations that can be executed.
 */
public interface Plan {

    /**
     * The plan version for serialization and debugging.
     *
     * @return
     */
    public fun getVersion(): Version = object : Version {
        override fun toString(): String = "1"
    }

    /**
     * The plan operation to execute.
     *
     * TODO consider `getOperations(): List<Operation>`.
     *
     * @return
     */
    public fun getOperation(): Operation
}
