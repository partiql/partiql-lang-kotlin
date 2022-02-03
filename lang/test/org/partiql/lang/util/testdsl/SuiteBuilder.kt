package org.partiql.lang.util.testdsl

import com.amazon.ion.IonValue
import org.junit.jupiter.api.assertDoesNotThrow
import org.partiql.lang.ION
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory

/** Builds a new test suite. */
@TestDslMarker
interface SuiteBuilder {
    /** Builds a new [IonResultTestGroup] for the test suite. */
    fun group(groupName: String, block: GroupBuilder.() -> Unit)

    infix fun String.hasVal(ionText: String): IonValue?
    fun parameterFactory(block: (ExprValueFactory) -> List<ExprValue>)
}

class SuiteBuilderImpl : SuiteBuilder {
    private val groups = mutableListOf<IonResultTestGroup>()
    private val globals = mutableMapOf<String, IonValue>()
    private var factoryBlock: (ExprValueFactory) -> List<ExprValue> = { listOf() }

    /** Builds a category to be added to the suite. */
    override fun group(groupName: String, block: GroupBuilder.() -> Unit) {
        groups.add(GroupBuilderImpl(groupName).apply(block).build())
    }

    override infix fun String.hasVal(ionText: String) =
        globals.put(this,
            assertDoesNotThrow("Parsing global variable '${this}' should not throw") {
                ION.singleValue(ionText)
            })

    override fun parameterFactory(block: (ExprValueFactory) -> List<ExprValue>) {
        factoryBlock = block
    }

    fun build() = IonResultTestSuite(globals.toMap(), groups.toList(), factoryBlock)
}
