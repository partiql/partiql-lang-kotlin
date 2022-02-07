package org.partiql.lang.schemadiscovery

import com.amazon.ion.IonReader
import org.partiql.ionschema.model.IonSchemaModel

/**
 * Infers a basic schema from a sequence of example data.
 */
interface SchemaInferencerFromExample {
    /**
     * Infers an [IonSchemaModel.Schema] from an [IonReader] using [maxExampleCount] examples.
     *
     * If a non-null [definiteISL] is provided, the discovered schema will also be unified with the definite schema.
     */
    fun inferFromExamples(reader: IonReader, maxExampleCount: Int, definiteISL: IonSchemaModel.Schema? = null): IonSchemaModel.Schema
}
