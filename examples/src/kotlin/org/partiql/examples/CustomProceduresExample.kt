package org.partiql.examples

import com.amazon.ion.IonDecimal
import com.amazon.ion.IonStruct
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.examples.util.Example
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedureSignature
import org.partiql.lang.eval.stringValue
import java.io.PrintStream
import java.math.BigDecimal
import java.math.RoundingMode

private val ion = IonSystemBuilder.standard().build()

/**
 * A simple custom stored procedure that calculates the moon weight for each crewmate of the given crew, storing the
 * moon weight in the [EvaluationSession] global bindings. This procedure also returns the number of crewmates we
 * calculated the moon weight for, returning -1 if no crew is found.
 *
 * This example demonstrates how to create a custom stored procedure, check argument types, and modify the
 * [EvaluationSession].
 */
class CalculateCrewMoonWeight(private val valueFactory: ExprValueFactory): StoredProcedure {
    private val MOON_GRAVITATIONAL_CONSTANT = BigDecimal(1.622 / 9.81)

    // [StoredProcedureSignature] takes two arguments:
    //   1. the name of the stored procedure
    //   2. the arity of this stored procedure. Checks to arity are taken care of by the evaluator. However, we must
    //      still check that the passed arguments are of the right type in our implementation of the procedure.
    override val signature = StoredProcedureSignature(name = "calculate_crew_moon_weight", arity = 1)

    // `call` is where you define the logic of the stored procedure given an [EvaluationSession] and a list of
    // arguments
    override fun call(session: EvaluationSession, args: List<ExprValue>): ExprValue {
        // We first check that the first argument is a string
        val crewName = args.first()
        // In the future the evaluator will also verify function argument types, but for now we must verify their type
        // manually
        if (crewName.type != ExprValueType.STRING) {
            val errorContext = PropertyValueMap().also {
                it[Property.EXPECTED_ARGUMENT_TYPES] = "STRING"
                it[Property.ACTUAL_ARGUMENT_TYPES] = crewName.type.name
                it[Property.FUNCTION_NAME] = signature.name
            }
            throw EvaluationException("First argument to ${signature.name} was not a string",
                ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false)
        }

        // Next we check if the given `crewName` is in the [EvaluationSession]'s global bindings. If not, we return 0.
        val sessionGlobals = session.globals
        val crewBindings = sessionGlobals[BindingName(crewName.stringValue(), BindingCase.INSENSITIVE)]
            ?: return valueFactory.newInt(-1)

        // Now that we've confirmed the given `crewName` is in the session's global bindings, we calculate and store
        // the moon weight for each crewmate in the crew.
        // In addition, we keep a running a tally of how many crewmates we do this for.
        var numCalculated = 0
        for (crewmateBinding in crewBindings) {
            val crewmate = crewmateBinding.ionValue as IonStruct
            val mass = crewmate["mass"] as IonDecimal
            val moonWeight = (mass.decimalValue() * MOON_GRAVITATIONAL_CONSTANT).setScale(1, RoundingMode.HALF_UP)
            crewmate.add("moonWeight", ion.newDecimal(moonWeight))

            numCalculated++
        }
        return valueFactory.newInt(numCalculated)
    }
}

/**
 * Demonstrates the use of custom stored procedure [CalculateCrewMoonWeight] in PartiQL queries.
 */
class CustomProceduresExample(out: PrintStream) : Example(out) {
    override fun run() {
        /**
         * To make custom stored procedures available to the PartiQL query being executed, they must be passed to
         * [CompilerPipeline.Builder.addProcedure].
         */
        val pipeline = CompilerPipeline.build(ion) {
            addProcedure(CalculateCrewMoonWeight(valueFactory))
        }

        // Here, we initialize the crews to be stored in our global session bindings
        val initialCrews = Bindings.ofMap(
            mapOf(
                "crew1" to pipeline.valueFactory.newFromIonValue(
                    ion.singleValue("""[ { name: "Neil",    mass: 80.5 }, 
                                         { name: "Buzz",    mass: 72.3 },
                                         { name: "Michael", mass: 89.9 } ]""")),
                "crew2" to pipeline.valueFactory.newFromIonValue(
                    ion.singleValue("""[ { name: "James", mass: 77.1 }, 
                                         { name: "Spock", mass: 81.6 } ]"""))
            )
        )
        val session = EvaluationSession.build { globals(initialCrews) }

        val crew1BindingName = BindingName("crew1", BindingCase.INSENSITIVE)
        val crew2BindingName = BindingName("crew2", BindingCase.INSENSITIVE)

        out.println("Initial global session bindings:")
        print("Crew 1:", "${session.globals[crew1BindingName]}")
        print("Crew 2:", "${session.globals[crew2BindingName]}")

        // We call our custom stored procedure using PartiQL's `EXEC` clause. Here we call our stored procedure
        // 'calculate_crew_moon_weight' with the arg 'crew1', which outputs the number of crewmates we've calculated
        // the moon weight for
        val procedureCall = "EXEC calculate_crew_moon_weight 'crew1'"
        val procedureCallOutput = pipeline.compile(procedureCall).eval(session)
        print("Number of calculated moon weights:", "$procedureCallOutput")

        out.println("Updated global session bindings:")
        print("Crew 1:", "${session.globals[crew1BindingName]}")
        print("Crew 2:", "${session.globals[crew2BindingName]}")
    }
}
