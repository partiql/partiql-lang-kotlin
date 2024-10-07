package org.partiql.planner.test

interface Test {

    /**
     * @return the name of the test.
     */
    fun getName(): String

    /**
     * Should throw an exception if a failure occurs.
     */
    fun assert()
}
