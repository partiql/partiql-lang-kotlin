
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.cli.utils.ServiceLoaderUtil
import org.partiql.lang.eval.ExprFunction

class ServiceLoaderUtilTest {
    @Test
    fun `loadPlugins loads the correct plugins`() {

        val functions: List<ExprFunction> = ServiceLoaderUtil.loadPlugins()

        assertEquals(1, functions.size)
        assertEquals(functions[0].signature.name, "trim_leading")
    }
}
