package org.partiql.planner.test

/**
 * Represents a unique identifier for a test case.
 *
 * This is used to uniquely identify a test case within a test suite.
 */
class TestId private constructor(
    private val _qualifier: Array<String>,
    private val _name: String
) {

    private fun getQualifier(): Array<String> {
        return _qualifier
    }

    private fun getName(): String {
        return _name
    }

    override fun toString(): String {
        return (getQualifier() + arrayOf(getName())).joinToString(".")
    }

    companion object {
        @JvmStatic
        fun of(vararg parts: String): TestId = of(parts.toList())

        @JvmStatic
        fun of(parts: Collection<String>): TestId {
            if (parts.isEmpty()) {
                error("Cannot create an identifier with no parts")
            }
            val qualifier = parts.take(parts.size - 1).toTypedArray()
            val identifier = parts.last()
            return TestId(qualifier, identifier)
        }
    }
}
