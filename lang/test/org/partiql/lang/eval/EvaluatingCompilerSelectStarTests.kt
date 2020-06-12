package org.partiql.lang.eval

import junitparams.Parameters
import org.junit.Test
import org.partiql.lang.util.downcast

class EvaluatingCompilerSelectStarTests : EvaluatorTestBase() {

    val session = EvaluationSession.build {
        globals(
            Bindings.ofMap(
                mapOf(
                    "dogs" to valueFactory.newBag(
                        sequenceOf(
                            createExprValue("""{ name: "fido" }""", 100, "addr0"),
                            createExprValue("""{ name: "bella" }""", 101, "addr1"),
                            createExprValue("""{ name: "max" }""", 102, "addr2"))))))
    }


    class AddressedExprValue(
        private val innerExprValue: ExprValue,
        override val name: ExprValue,
        override val address: ExprValue
    ) : ExprValue by innerExprValue, Named, Addressed {

        // Need to override the asFacet provided by [innerExprValue] since it won't implement either facet.
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> asFacet(facetType: Class<T>?): T? = downcast(facetType)
    }

    private fun createExprValue(ionText: String, index: Long, address: String) =
        AddressedExprValue(
            IonExprValue(valueFactory, ion.singleValue(ionText)),
            valueFactory.newInt(index),
            valueFactory.newString(address))

    @Test
    @Parameters
    fun tests(tc: EvaluatorTestCase) =
        runTestCase(tc, session)

    fun parametersForTests() =
        listOf(
            // SELECT * with AT projects the AT binding,
            EvaluatorTestCase(
                query = "SELECT * FROM dogs AT idx",
                expectedSql = """<< 
                        { 'name': 'fido', 'idx': 100 }, 
                        { 'name': 'bella', 'idx': 101 },
                        { 'name': 'max', 'idx': 102 } 
                    >>"""),
            // SELECT * with BY projects the BY binding,
            EvaluatorTestCase(
                query = "SELECT * FROM dogs BY addr",
                expectedSql = """<< 
                        { 'name': 'fido', 'addr': 'addr0' }, 
                        { 'name': 'bella', 'addr': 'addr1' },
                        { 'name': 'max', 'addr': 'addr2' } 
                    >>"""),
            // SELECT * with both AT and BY projects both,
            EvaluatorTestCase(
                query = "SELECT * FROM dogs AT idx BY addr",
                expectedSql = """<< 
                        { 'name': 'fido', 'addr': 'addr0', 'idx': 100 }, 
                        { 'name': 'bella', 'addr': 'addr1', 'idx': 101 },
                        { 'name': 'max', 'addr': 'addr2', 'idx': 102 } 
                    >>""")
        )

    @Test
    fun `select * over table with mixed types` () {
        runTestCase(
            EvaluatorTestCase(
                query = "select f.* from << { 'bar': 1 }, 10, << 11, 12 >> >> as f",
                expectedSql = """<< { 'bar': 1 } ,{ '_1': 10 }, { '_1': <<11, 12>> } >>"""),
                session = EvaluationSession.standard())
    }


    //
}


