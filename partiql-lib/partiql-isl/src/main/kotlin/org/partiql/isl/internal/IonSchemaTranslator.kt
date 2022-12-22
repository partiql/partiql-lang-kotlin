package org.partiql.isl.internal

import com.amazon.ionschema.Import
import com.amazon.ionschema.Schema
import org.partiql.isl.builder.IonSchema
import org.partiql.isl.builder.IonSchemaFactory
import org.partiql.isl.model.Footer
import org.partiql.isl.model.Version

/**
 * Translates from [com.amazon.ionschema.Schema] to [org.partiql.isl.model.Schema]
 */
internal class IonSchemaTranslator(private val factory: IonSchemaFactory) {

    fun translate(schema: Schema) = IonSchema.build(factory) {
        schema {
            version = Version.V2_0
            header = header {
                imports = schema.getImports().
                schema.getImports().forEach {
                    val schema = it.getSchema()
                    val id = it.id
                }
            }
            // Definitions
            // TODO
            // Footer
            footer = Footer()
        }
    }

    private fun IonSchema.Builder.translate(import: Import) = import {

    }

}
