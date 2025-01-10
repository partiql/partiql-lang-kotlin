package org.partiql.parser.internal

interface PTestDef {
    /**
     * Returns the name of the test
     */
    fun name(): String

    /**
     * Runs the test case.
     */
    fun assert()
}
