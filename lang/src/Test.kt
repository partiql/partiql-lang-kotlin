import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.syntax.SqlParser

fun main(){
    val ion = IonSystemBuilder.standard().build()
    val pipeline = CompilerPipeline.standard(ion)

    val globalVariables = Bindings.buildLazyBindings<ExprValue> {
        addBinding("users") {
            val values = ion.loader.load(
                """
        {
        "name": "mustafa",
        "age": 30,
        }
        {
        "name": "ilteris",
        "age": 26,
        }
        {
        "name": "onur",
        "age": 24,
        }""".trimMargin()
            )
            pipeline.valueFactory.newFromIonValue(values)
        }
        addBinding("roles") {
            val values = ion.loader.load(
                """
        {
        "name": "mustafa",
        "admin": true,
        }
        {
        "name": "ilteris",
        "admin": false,
        }""".trimMargin()
            )
            pipeline.valueFactory.newFromIonValue(values)
        }
    }

    //val query = "SELECT * FROM users WHERE name IN (SELECT VALUE name FROM roles)"
    //val query = "SELECT * FROM users WHERE EXISTS (SELECT * FROM roles WHERE roles.name = users.name)"
    val query = "SELECT name FROM roles"

    val parser = SqlParser(ion)
    val ast = parser.parseAstStatement(query)

    val e = pipeline.compile(query)
    val session = EvaluationSession.build {
        globals(globalVariables)
    }

    val result = e.eval(session)

    println(result)

//    for (exprValue in result) {
//        println(exprValue)
//    }
}