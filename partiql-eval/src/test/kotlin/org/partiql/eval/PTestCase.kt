package org.partiql.eval

interface PTestCase : Runnable {
    /**
     * Executes the test case
     */
    override fun run()
}
