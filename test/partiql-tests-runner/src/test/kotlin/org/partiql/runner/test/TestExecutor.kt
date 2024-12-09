package org.partiql.runner.test

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.runner.CompileType

interface TestExecutor<T, V> {

    /**
     * Compile the given statement.
     *
     * @param input
     * @return
     */
    fun prepare(input: String): T

    /**
     * Execute the statement, returning a value we can assert on.
     *
     * @param input
     * @return
     */
    fun execute(input: T): V

    /**
     * Compare the equality of two values.
     *
     * @param actual
     * @param expect
     */
    fun compare(actual: V, expect: V): Boolean

    /**
     * Read an IonValue to the value type [V] used by this executor.
     *
     * @param value
     * @return
     */
    fun fromIon(value: IonValue): V

    /**
     * Write a value [V] to an IonValue for debug printing.
     *
     * @param value
     * @return
     */
    fun toIon(value: V): IonValue

    /**
     * CompileOptions varies for each test, need a way to programmatically create an executor.
     *
     * @param T
     * @param V
     */
    interface Factory<T, V> {

        fun create(env: IonStruct, options: CompileType): TestExecutor<T, V>
    }
}
