package org.partiql.planner.internal

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Catalog
<<<<<<< HEAD
import org.partiql.planner.internal.ir.Rex
=======
>>>>>>> partiql-spi
import org.partiql.planner.internal.typer.TypeEnv
import org.partiql.plugins.local.LocalConnector
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import java.util.Random
import kotlin.io.path.toPath
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EnvTest {

    companion object {

        private val root = this::class.java.getResource("/catalogs/default/pql")!!.toURI().toPath()

        private val EMPTY_TYPE_ENV = TypeEnv(schema = emptyList())

        private val GLOBAL_OS = Catalog(
            name = "pql",
            symbols = listOf(
                Catalog.Symbol(path = listOf("main", "os"), type = StaticType.STRING)
            )
        )
    }

    private lateinit var env: Env

    @BeforeEach
    fun init() {
        env = Env(
            PartiQLPlanner.Session(
                queryId = Random().nextInt().toString(),
                userId = "test-user",
                currentCatalog = "pql",
                currentDirectory = listOf("main"),
                catalogs = mapOf(
                    "pql" to LocalConnector.Metadata(root)
                ),
            )
        )
    }

    @Test
    fun testGlobalMatchingSensitiveName() {
        val path = BindingPath(listOf(BindingName("os", BindingCase.SENSITIVE)))
<<<<<<< HEAD
        assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assertEquals(1, env.symbols.size)
        assert(env.symbols.contains(GLOBAL_OS))
=======
        assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, ResolutionStrategy.GLOBAL))
        assertEquals(1, env.catalogs.size)
        assert(env.catalogs.contains(GLOBAL_OS))
>>>>>>> partiql-spi
    }

    @Test
    fun testGlobalMatchingInsensitiveName() {
        val path = BindingPath(listOf(BindingName("oS", BindingCase.INSENSITIVE)))
<<<<<<< HEAD
        assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assertEquals(1, env.symbols.size)
        assert(env.symbols.contains(GLOBAL_OS))
=======
        assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, ResolutionStrategy.GLOBAL))
        assertEquals(1, env.catalogs.size)
        assert(env.catalogs.contains(GLOBAL_OS))
>>>>>>> partiql-spi
    }

    @Test
    fun testGlobalNotMatchingSensitiveName() {
        val path = BindingPath(listOf(BindingName("oS", BindingCase.SENSITIVE)))
<<<<<<< HEAD
        assertNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assert(env.symbols.isEmpty())
=======
        assertNull(env.resolve(path, EMPTY_TYPE_ENV, ResolutionStrategy.GLOBAL))
        assert(env.catalogs.isEmpty())
>>>>>>> partiql-spi
    }

    @Test
    fun testGlobalNotMatchingInsensitiveName() {
        val path = BindingPath(listOf(BindingName("nonexistent", BindingCase.INSENSITIVE)))
<<<<<<< HEAD
        assertNull(env.resolve(path, EMPTY_TYPE_ENV, Rex.Op.Var.Scope.DEFAULT))
        assert(env.symbols.isEmpty())
=======
        assertNull(env.resolve(path, EMPTY_TYPE_ENV, ResolutionStrategy.GLOBAL))
        assert(env.catalogs.isEmpty())
>>>>>>> partiql-spi
    }
}
