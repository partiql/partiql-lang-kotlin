
package org.partiql.cli.functions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.cli.makeCliAndGetResult
import org.partiql.cli.pipeline.AbstractPipeline
import org.partiql.cli.utils.ServiceLoaderUtil
import java.nio.file.Paths

/**
 * Class `TrimLeadTest` is used to test the 'trim_lead' function, which is used to trim the leading whitespace characters
 * from the string it processes. It is a plugin mockdb functions loaded by Java Service Loader.
 *
 * @property pipeline Creates a pipeline using service loaded functions. It allows to process a stream of records.
 *
 * @constructor Creates an instance of `TrimLeadTest`.
 */
@Disabled
class TrimLeadTest {

    val pluginPath = Paths.get(System.getProperty("testingPluginDirectory"))

    private val pipeline = AbstractPipeline.create(
        AbstractPipeline.PipelineOptions(
            functions = ServiceLoaderUtil.loadFunctions(pluginPath)
        )
    )

    @Test
    fun TrimTest() {
        val input = "'   hello'"
        val expected = "\"hello\""

        val result = makeCliAndGetResult(query = "trim_lead($input)", pipeline = pipeline)

        assertEquals(expected, result.trim())
    }
}
