
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.cli.utils.ServiceLoaderUtil
import org.partiql.lang.eval.ExprFunction

class ServiceLoaderUtilTest {
    @Test
    fun `loadPlugins loads the correct plugins`() {

        val functions: List<ExprFunction> = ServiceLoaderUtil.loadFunctions()

        assertTrue(functions.map { it.signature.name }.contains("trim_lead"))
        assertTrue(functions.map { it.signature.name }.contains("test_power"))
    }
}
