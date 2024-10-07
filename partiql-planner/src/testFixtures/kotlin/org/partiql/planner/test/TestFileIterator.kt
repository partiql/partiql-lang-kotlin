package org.partiql.planner.test

import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.createIonElementLoader
import java.io.InputStream

/**
 * Iterates over the tests defined in a file.
 */
internal class TestFileIterator(
    private val path: List<String>,
    stream: InputStream,
    private val factory: TestBuilderFactory,
    private val inputProvider: PartiQLTestProvider
) : Iterator<Test> {

    private val reader = IonReaderBuilder.standard().build(stream)
    private val loader = createIonElementLoader()
    private var _type = reader.next()

    override fun hasNext(): Boolean {
        return _type != null
    }

    override fun next(): Test {
        val element = loader.loadCurrentElement(reader)
        _type = reader.next()
        val struct = element.asStruct()
        val name = struct["name"].asString().textValue
        val type = struct["type"].asString().textValue
        val payload = struct["body"].asStruct()
        val testId = TestId.of(path + listOf(name))
        return factory[type]?.id(testId)?.config(payload, inputProvider)?.build() ?: error("Could not find test builder for: $type")
    }
}
