package org.partiql.planner.internal

class EnvTest {

    // companion object {
    //
    //     private val root = this::class.java.getResource("/catalogs/default/pql")!!.toURI().toPath()
    //
    //     private val EMPTY_TYPE_ENV = TypeEnv(schema = emptyList())
    //
    //     private val GLOBAL_OS = Catalog(
    //         name = "pql",
    //         symbols = listOf(
    //             Catalog.Symbol(path = listOf("main", "os"), type = StaticType.STRING)
    //         )
    //     )
    // }
    //
    // private lateinit var env: Env
    //
    // @BeforeEach
    // fun init() {
    //     env = Env(
    //         PartiQLPlanner.Session(
    //             queryId = Random().nextInt().toString(),
    //             userId = "test-user",
    //             currentCatalog = "pql",
    //             currentDirectory = listOf("main"),
    //             catalogs = mapOf(
    //                 "pql" to LocalConnector.Metadata(root)
    //             ),
    //         )
    //     )
    // }
    //
    // @Test
    // fun testGlobalMatchingSensitiveName() {
    //     val path = BindingPath(listOf(BindingName("os", BindingCase.SENSITIVE)))
    //     assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, Scope.GLOBAL))
    //     assertEquals(1, env.catalogs.size)
    //     assert(env.catalogs.contains(GLOBAL_OS))
    // }
    //
    // @Test
    // fun testGlobalMatchingInsensitiveName() {
    //     val path = BindingPath(listOf(BindingName("oS", BindingCase.INSENSITIVE)))
    //     assertNotNull(env.resolve(path, EMPTY_TYPE_ENV, Scope.GLOBAL))
    //     assertEquals(1, env.catalogs.size)
    //     assert(env.catalogs.contains(GLOBAL_OS))
    // }
    //
    // @Test
    // fun testGlobalNotMatchingSensitiveName() {
    //     val path = BindingPath(listOf(BindingName("oS", BindingCase.SENSITIVE)))
    //     assertNull(env.resolve(path, EMPTY_TYPE_ENV, Scope.GLOBAL))
    //     assert(env.catalogs.isEmpty())
    // }
    //
    // @Test
    // fun testGlobalNotMatchingInsensitiveName() {
    //     val path = BindingPath(listOf(BindingName("nonexistent", BindingCase.INSENSITIVE)))
    //     assertNull(env.resolve(path, EMPTY_TYPE_ENV, Scope.GLOBAL))
    //     assert(env.catalogs.isEmpty())
    // }
}
