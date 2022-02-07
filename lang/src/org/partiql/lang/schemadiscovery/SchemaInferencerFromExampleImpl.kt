package org.partiql.lang.schemadiscovery

import com.amazon.ion.IonReader
import com.amazon.ion.IonSequence
import com.amazon.ion.IonStruct
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionschema.IonSchemaSystem
import com.amazon.ionschema.Type
import org.partiql.ionschema.model.IonSchemaModel
import org.partiql.lang.util.stringValueOrNull

/**
 * Implementation for [SchemaInferencerFromExample]. Requires a [typeName] for the generated schema's top level type
 * name. Also requires an [IonSchemaSystem] and [schemaIds] to load additional schema types that will be used in the
 * generated schema. The passed [schemaIds] will also be used for the generated
 * [IonSchemaModel.SchemaStatement.HeaderStatement]'s [IonSchemaModel.ImportList].
 */
class SchemaInferencerFromExampleImpl(val typeName: String, iss: IonSchemaSystem, val schemaIds: List<String>): SchemaInferencerFromExample {
    private val importedTypes = schemaIds.loadImportedTypes(iss)
    private val sequenceTypes = importedTypes.loadSequenceTypes()
    private val islAnyConstraints = IonSchemaModel.build { constraintList() }
    private val islAnySchema = IonSchemaModel.build {
        schema(
            headerStatement(openFieldList(), importList(schemaIds.map { import(it) })),
            typeStatement(typeDefinition(typeName, islAnyConstraints)),
            footerStatement(openFieldList()))
    }

    override fun inferFromExamples(reader: IonReader, maxExampleCount: Int, definiteISL: IonSchemaModel.Schema?): IonSchemaModel.Schema {
        val parser = IonExampleParser(IonSystemBuilder.standard().build())
        if (maxExampleCount < 1) {
            return islAnySchema
        }

        val firstExample = parser.parseExample(reader) ?: return islAnySchema
        val examples = mutableListOf(firstExample)

        var example = parser.parseExample(reader)
        var numExamplesLeft = maxExampleCount - 1
        while (example != null && numExamplesLeft > 0) {
            examples.add(example)
            example = parser.parseExample(reader)
            numExamplesLeft--
        }

        val dataguideConstraintUnifier = ConstraintUnifier.builder()
            .sequenceTypes(sequenceTypes)
            .discoveredConstraintUnifier(MultipleTypedDCU(constraintUnifiers = standardTypedDiscoveredConstraintUnifiers))
            .build()

        val dataguideInferer = TypeAndConstraintInferer(
            constraintUnifier = dataguideConstraintUnifier,
            constraintDiscoverer = StandardConstraintDiscoverer(),
            importedTypes = importedTypes)

        val discoveredWithDefiniteUnifier = ConstraintUnifier.builder()
            .sequenceTypes(sequenceTypes)
            .discoveredConstraintUnifier(AppendAdditionalConstraints())
            .build()

        val unifiedTypeConstraint = examples.asSequence()
            .map { dataguideInferer.inferConstraints(it) }
            .unifiedConstraintList(dataguideInferer.constraintUnifier)
            .let { NormalizeNullableVisitorTransform().transformConstraintList(it) }
            .let { discoveredConstraints ->
                when (definiteISL) {
                    null -> discoveredConstraints
                    else -> {
                        val definiteSchemaTypeStatement = definiteISL.getFirstTypeStatement()
                        val definiteISLTopTypeName = definiteSchemaTypeStatement.typeDef.name?.text
                        if (typeName != definiteISLTopTypeName) {
                            error("""Top level type name differs.
                                     Expected: $typeName
                                     Actual:   $definiteISLTopTypeName""".trimIndent())
                        }
                        discoveredWithDefiniteUnifier.unify(discoveredConstraints, definiteSchemaTypeStatement.typeDef.constraints)
                    }
                }
            }

        return IonSchemaModel.build {
            schema(
                headerStatement(openFieldList(), importList(schemaIds.map { import(it) })),
                typeStatement(typeDefinition(name = typeName, constraints = unifiedTypeConstraint)),
                footerStatement(openFieldList())
            )
        }
    }

    /**
     * Returns a list of all the [Type]s in [this] list of schema identifiers.
     */
    private fun List<String>.loadImportedTypes(iss: IonSchemaSystem): List<Type> {
        val schemas = this.map { schemaId -> iss.loadSchema(schemaId) }
        return schemas.flatMap { schema -> schema.getTypes().asSequence().toList() }
    }

    /**
     * Returns the names of all the [IonSequence]s (i.e. list, sexp) and imported sequence types.
     */
    private fun List<Type>.loadSequenceTypes(): List<String> {
        return this.fold(mutableListOf(TypeConstraint.LIST.typeName, TypeConstraint.SEXP.typeName)) { acc, t ->
            val typeAsStruct = t.isl as IonStruct
            val definedType = typeAsStruct.get("type").stringValueOrNull()
            if (definedType == TypeConstraint.LIST.typeName || definedType == TypeConstraint.SEXP.typeName) {
                acc.add(t.name)
            }
            acc
        }
    }
}
