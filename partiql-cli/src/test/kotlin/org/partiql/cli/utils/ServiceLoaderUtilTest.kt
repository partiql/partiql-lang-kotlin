
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.cli.utils.ServiceLoaderUtil
import org.partiql.lang.eval.ExprFunction
import java.nio.file.Paths

class ServiceLoaderUtilTest {
    @Test
    fun `loadPlugins loads the correct plugins`() {

        val pluginPath = Paths.get(System.getProperty("testingPluginDirectory"))
        val functions: List<ExprFunction> = ServiceLoaderUtil.loadFunctions(pluginPath)

        assertTrue(functions.map { it.signature.name }.contains("trim_lead"))
        assertTrue(functions.map { it.signature.name }.contains("test_power"))
    }
}
