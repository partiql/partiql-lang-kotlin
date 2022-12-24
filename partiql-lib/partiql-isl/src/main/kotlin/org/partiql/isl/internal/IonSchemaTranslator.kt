package org.partiql.isl.internal

import com.amazon.ionschema.Import
import com.amazon.ionschema.Schema
import com.amazon.ionschema.Type
import org.partiql.isl.Version
import org.partiql.isl.builder.IonSchema
import org.partiql.isl.builder.IonSchemaFactory

/**
 * Translates from [com.amazon.ionschema.Schema] to [org.partiql.isl.model.Schema]
 *
 * TODO we cannot translate from ionschema.Schema because certain fields (user_reserved_fields, constraints, ?) are not
 *   exposed in the ionschema.Schema. Check with Matt about leveraging SchemaImpl_2_0 because we shouldn't write an
 *   additional ISL parser.
 */
internal class IonSchemaTranslator(private val factory: IonSchemaFactory) {

    /*
     * Attempting to resemble the document structure
     */
    fun translate(schema: Schema) = IonSchema.build(factory) {
        schema {
            version = Version.V2_0
            header = header {
                for (import in schema.getImports()) {
                    imports += translate(import)
                }
                userReservedFields = userReservedFields {}
            }
            for (type in schema.getTypes()) {
                definitions += translate(type)
            }
            footer = footer {}
        }
    }

    /**
     * Translate from an IonSchema import to an ISL Import
     */
    private fun IonSchema.Builder.translate(import: Import): List<org.partiql.isl.Import> {
        TODO("Cannot translate without looking at IonValue. The constraints are not exposed")
    }

    private fun IonSchema.Builder.translate(type: Type) = definition {
        TODO("Cannot translate without looking at IonValue. The constraints are not exposed")
    }
}
