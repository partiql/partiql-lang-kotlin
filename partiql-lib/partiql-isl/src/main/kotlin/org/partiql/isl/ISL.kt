package org.partiql.isl

import com.amazon.ion.IonValue
import com.amazon.ionelement.api.IonElement
import com.amazon.ionschema.IonSchemaSystem
import com.amazon.ionschema.IonSchemaSystemBuilder
import org.partiql.isl.builder.IonSchemaFactory
import org.partiql.isl.internal.IonSchemaTranslator
import org.partiql.isl.internal.IonSchemaWriter

object ISL {

    private val ISS = IonSchemaSystemBuilder.standard().build()

    /**
     * Constructs a new [Schema] using ISL provided as a String.
     *
     * @param isl       ISL string representation of the schema to create
     * @param iss       IonSchemaSystem
     * @param factory   IonSchemaFactory
     * @return
     */
    fun fromIon(
        isl: String,
        iss: IonSchemaSystem = ISS,
        factory: IonSchemaFactory = IonSchemaFactory.DEFAULT,
    ): Schema = IonSchemaTranslator(factory).translate(iss.newSchema(isl))

    /**
     * Constructs a new [Schema] using ISL provided as Iterator<IonValue>
     *
     * @param isl       ISL string representation of the schema to create
     * @param iss       IonSchemaSystem
     * @param factory   IonSchemaFactory
     * @return
     */
    fun fromIon(
        isl: Iterator<IonValue>,
        iss: IonSchemaSystem = ISS,
        factory: IonSchemaFactory = IonSchemaFactory.DEFAULT,
    ): Schema = IonSchemaTranslator(factory).translate(iss.newSchema(isl))

    /**
     * Returns an IonElement representing the given schema
     *
     * @param schema
     * @return
     */
    fun toIon(schema: Schema): Iterator<IonElement> = IonSchemaWriter.toIon(schema)
}
