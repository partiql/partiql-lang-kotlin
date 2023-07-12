
package org.partiql.cli.functions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.cli.makeCliAndGetResult
import org.partiql.cli.pipeline.AbstractPipeline
import org.partiql.cli.utils.ServiceLoaderUtil

/**
 * Class `PowTest` is used to test the 'test_power' function, which calculates the base to the power of exponent.
 * It is a plugin mockdb functions loaded by Java Service Loader.
 *
 * @property pipeline Creates a pipeline using service loaded functions. It allows to process a stream of records.
 *
 * @constructor Creates an instance of `PowTest`.
 */
class PowTest {

    private val pipeline = AbstractPipeline.create(
        AbstractPipeline.PipelineOptions(
            functions = ServiceLoaderUtil.loadFunctions()
        )
    )

    @Test
    fun PowTest() {
        val result = makeCliAndGetResult(query = "test_power(2,3)", pipeline = pipeline)
        assertEquals(8.0, result.toDouble())
    }
}
