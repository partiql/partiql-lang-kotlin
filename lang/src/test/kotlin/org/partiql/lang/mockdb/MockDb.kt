package org.partiql.lang.mockdb

import com.amazon.ion.IonValue
import com.amazon.ionschema.IonSchemaSystem
import org.partiql.lang.ION
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.toExprValue
import org.partiql.lang.mappers.StaticTypeMapper
import org.partiql.lang.schemadiscovery.NormalizeDecimalPrecisionsToUpToRange
import org.partiql.lang.schemadiscovery.SchemaInferencerFromExampleImpl
import org.partiql.lang.types.StaticType

/**
 * Encapsulates the information needed to provide a "mock" database to our evaluation tests.
 *
 * A mock database consists of tables and other global variables.  The types and values of the global
 * exposed thru the [valueBindings] and [typeBindings] properties.
 *
 * When using this class, the [typeBindings] does not need to be constructed manually--types are
 * synthesized using [SchemaInferencerFromExampleImpl] to infer a [StaticType] for each global variable.
 */
class MockDb(
    val globals: Map<String, IonValue>,
    iss: IonSchemaSystem
) {
    /**
     * Provides an implementation of [Bindings<ExprValue>] for accessing the values of tables and
     * other global variables of our mock database.
     */
    val valueBindings: Bindings<ExprValue> = Bindings.ofMap(
        globals.mapValues { it.value.toExprValue() }
    )

    /**
     * Provides an implementation of [Bindings<StaticType>] for accessing the data types of tables and other
     * global variables of our mock database.
     */
    val typeBindings: Bindings<StaticType> = Bindings.ofMap(
        this.globals.mapValues {
            // TODO: consider creating a factory for [SchemaInferencerFromExampleImpl] to hide its concrete
            //  implementation?
            val schemaModel = SchemaInferencerFromExampleImpl(it.key, iss, listOf("partiql.isl"))
                .inferFromExamples(ION.newReader(it.value), Int.MAX_VALUE)
            // some inferred constraints need normalized to a format suitable to be mapped to PartiQL's static types
            // currently the dataguide infers exact decimal precision values and ranges, where as PartiQL models
            // "upto" decimal precision ranges
            val normalizedSchemaModel = NormalizeDecimalPrecisionsToUpToRange().transformSchema(schemaModel)
            StaticTypeMapper(normalizedSchemaModel).toStaticType(it.key)
        }.toMap()
    )

    fun toSession(): EvaluationSession = EvaluationSession.build { globals(valueBindings) }
}
