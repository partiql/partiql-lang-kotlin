package org.partiql.planner

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.plan.Rex
import org.partiql.plan.identifierQualified
import org.partiql.plan.identifierSymbol
import org.partiql.plugins.local.LocalPlugin
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import java.util.Random
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EnvTest {

    companion object {
        private val root = this::class.java.getResource("/catalogs/default")!!.toURI().toPath().pathString

        val catalogConfig = mapOf(
            "pql" to ionStructOf(
                field("connector_name", ionString("local")),
                field("root", ionString("$root/pql")),
            )
        )

        private val EMPTY_TYPE_ENV = TypeEnv(schema = emptyList(), ResolutionStrategy.GLOBAL)

        private val GLOBAL_OS = Global(
            path = identifierQualified(
                root = identifierSymbol("pql", Identifier.CaseSensitivity.SENSITIVE),
                steps = listOf(
                    identifierSymbol("main", Identifier.CaseSensitivity.SENSITIVE),
                    identifierSymbol("os", Identifier.CaseSensitivity.SENSITIVE)
                )
            ),
            type = StaticType.STRING
        )
    }

    private lateinit var env: Env

    @BeforeEach
    fun init() {
        env = Env(
            listOf(PartiQLHeader),
            listOf(LocalPlugin()),
            PartiQLPlanner.Session(
                queryId = Random().nextInt().toString(),
                userId = "test-user",
                currentCatalog = "pql",
                currentDirectory = listOf("main"),
                catalogConfig = catalogConfig
            )
        )
    }

    @Test
    fun testGlobalMatchingSensitiveName() {
        val path = BindingPath(listOf(BindingName("os", BindingCase.SENSITIVE)))
        assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assertEquals(1, env.globals.size)
        assert(env.globals.contains(GLOBAL_OS))
    }

    @Test
    fun testGlobalMatchingInsensitiveName() {
        val path = BindingPath(listOf(BindingName("oS", BindingCase.INSENSITIVE)))
        assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assertEquals(1, env.globals.size)
        assert(env.globals.contains(GLOBAL_OS))
    }

    @Test
    fun testGlobalNotMatchingSensitiveName() {
        val path = BindingPath(listOf(BindingName("oS", BindingCase.SENSITIVE)))
        assertNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assert(env.globals.isEmpty())
    }

    @Test
    fun testGlobalNotMatchingInsensitiveName() {
        val path = BindingPath(listOf(BindingName("nonexistent", BindingCase.INSENSITIVE)))
        assertNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assert(env.globals.isEmpty())
    }
}
