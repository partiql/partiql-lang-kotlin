package org.partiql.ionschema.model

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol

private val MAX = ionSymbol("max")
private val MIN = ionSymbol("min")

/**
 * Transforms a PIG-generated [IonSchemaModel.Schema] into an [IonElement] representation of an ISL document.
 *
 * @receiver [IonSchemaModel.Schema] to be transformed to an ISL document
 * @return transformed ISL document represented as a List<[AnyElement]>
 */
fun IonSchemaModel.Schema.toIsl(): List<AnyElement> =
    this.statements.map { stmt ->
        when (stmt) {
            is IonSchemaModel.SchemaStatement.HeaderStatement -> {
                val fields = listOfNotNull(
                    stmt.imports?.let { imports ->
                        field("imports", ionListOf(imports.items.map { i -> i.toIsl() }))
                    },
                    *stmt.openContent.toStructFields().toTypedArray()
                )

                ionStructOf(fields, annotations = listOf("schema_header")).asAnyElement()
            }
            is IonSchemaModel.SchemaStatement.FooterStatement ->
                ionStructOf(
                    stmt.openContent.toStructFields(),
                    annotations = listOf("schema_footer")
                )
                    .asAnyElement()
            is IonSchemaModel.SchemaStatement.TypeStatement -> stmt.typeDef.toIsl(isInline = false).asAnyElement()
            is IonSchemaModel.SchemaStatement.ContentStatement -> stmt.value
        }
    }

private fun IonSchemaModel.Import.toIsl(): AnyElement =
    ionStructOf(
        listOfNotNull(
            field("id", ionSymbol(this.id.text)),
            this.typeName?.let { field("type", ionSymbol(it.text)) },
            this.alias?.let { field("as", ionSymbol(it.text)) }
        )
    ).asAnyElement()

private fun IonSchemaModel.OpenFieldList.toStructFields(): List<StructField> =
    this.contents.map { field(it.name.text, it.value) }

/**
 * Transforms a PIG-generated [IonSchemaModel.TypeDefinition] into a [StructElement] representing the ISL type
 * definition.
 *
 * @receiver [IonSchemaModel.TypeDefinition] that will be transformed into an ISL type definition
 * @param isInline indicates if [this] is an inline type. If false, adds the "type" annotation to the returned ISL type
 *  definition
 * @return transformed ISL document represented as a [StructElement]
 */
fun IonSchemaModel.TypeDefinition.toIsl(isInline: Boolean): StructElement {
    val name = name?.text
    val nameField = name?.let { listOf(field("name", ionSymbol(it))) } ?: listOf()
    val typeStruct = ionStructOf(nameField + this.constraints.items.map { it.toIsl() })

    return if (!isInline) {
        typeStruct.withAnnotations("type")
    } else {
        typeStruct
    }
}

private fun IonSchemaModel.Constraint.toIsl(): StructField {
    return when (this) {
        is IonSchemaModel.Constraint.CodepointLength -> field("codepoint_length", this.rule.toIsl())
        is IonSchemaModel.Constraint.ByteLength -> field("byte_length", this.rule.toIsl())
        is IonSchemaModel.Constraint.ContainerLength -> field("container_length", this.rule.toIsl())
        is IonSchemaModel.Constraint.ClosedContent -> field("content", ionSymbol("closed"))
        is IonSchemaModel.Constraint.Element -> field("element", this.type.toIsl())
        is IonSchemaModel.Constraint.Fields -> field("fields", ionStructOf(this.fields.map { field(it.name.text, it.type.toIsl()) }))
        is IonSchemaModel.Constraint.Precision -> field("precision", this.rule.toIsl())
        is IonSchemaModel.Constraint.Scale -> field("scale", this.rule.toIsl())
        is IonSchemaModel.Constraint.TypeConstraint -> field("type", this.type.toIsl())
        is IonSchemaModel.Constraint.Occurs -> field("occurs", this.spec.toIsl())
        is IonSchemaModel.Constraint.ValidValues -> field("valid_values", this.spec.toIsl())
        is IonSchemaModel.Constraint.Regex -> field("regex", this.toIsl())
        is IonSchemaModel.Constraint.Contains -> field("contains", ionListOf(this.values))
        is IonSchemaModel.Constraint.Not -> field("not", this.type.toIsl())
        is IonSchemaModel.Constraint.OneOf -> field("one_of", this.types.toIsl())
        is IonSchemaModel.Constraint.AllOf -> field("all_of", this.types.toIsl())
        is IonSchemaModel.Constraint.AnyOf -> field("any_of", this.types.toIsl())
        is IonSchemaModel.Constraint.OrderedElements -> field("ordered_elements", this.types.toIsl())
        is IonSchemaModel.Constraint.Annotations -> field("annotations", this.toIsl())
        is IonSchemaModel.Constraint.TimestampPrecision -> field("timestamp_precision", this.precision.toIsl())
        is IonSchemaModel.Constraint.TimestampOffset -> field("timestamp_offset", ionListOf(this.offsetPatterns.map { ionString(it.text) }))
        is IonSchemaModel.Constraint.Utf8ByteLength -> field("utf8_byte_length", this.rule.toIsl())
        is IonSchemaModel.Constraint.ArbitraryConstraint -> field(this.name.text, this.value)
    }
}

private fun IonSchemaModel.TsPrecision.toIsl(): IonElement =
    when (this) {
        is IonSchemaModel.TsPrecision.EqualsTsPrecisionRange -> this.toIsl()
        is IonSchemaModel.TsPrecision.EqualsTsPrecisionValue -> this.value.toIsl()
    }

private fun IonSchemaModel.TsPrecision.EqualsTsPrecisionRange.toIsl(): IonElement =
    ionListOf(this.range.min.toIsl(), this.range.max.toIsl(), annotations = listOf("range"))

private fun IonSchemaModel.TsPrecisionExtent.toIsl(): IonElement =
    when (this) {
        is IonSchemaModel.TsPrecisionExtent.MinTsp -> ionSymbol("min")
        is IonSchemaModel.TsPrecisionExtent.MaxTsp -> ionSymbol("max")
        is IonSchemaModel.TsPrecisionExtent.InclusiveTsp -> this.precision.toIsl()
        is IonSchemaModel.TsPrecisionExtent.ExclusiveTsp -> this.precision.toIsl(listOf("exclusive"))
    }

private fun IonSchemaModel.TsPrecisionValue.toIsl(annotations: List<String> = emptyList()) =
    when (this) {
        is IonSchemaModel.TsPrecisionValue.Year -> ionSymbol("year", annotations)
        is IonSchemaModel.TsPrecisionValue.Month -> ionSymbol("month", annotations)
        is IonSchemaModel.TsPrecisionValue.Day -> ionSymbol("day", annotations)
        is IonSchemaModel.TsPrecisionValue.Minute -> ionSymbol("minute", annotations)
        is IonSchemaModel.TsPrecisionValue.Second -> ionSymbol("second", annotations)
        is IonSchemaModel.TsPrecisionValue.Millisecond -> ionSymbol("millisecond", annotations)
        is IonSchemaModel.TsPrecisionValue.Microsecond -> ionSymbol("microsecond", annotations)
        is IonSchemaModel.TsPrecisionValue.Nanosecond -> ionSymbol("nanosecond", annotations)
    }

private fun List<IonSchemaModel.TypeReference>.toIsl(): IonElement =
    ionListOf(this.map { it.toIsl() })

private fun IonSchemaModel.Constraint.Regex.toIsl() =
    ionString(
        this.pattern.text,
        annotations = listOfNotNull(
            "i".takeIf { this.caseInsensitive.booleanValue },
            "m".takeIf { this.multiline.booleanValue }
        )
    )

private fun IonSchemaModel.Constraint.Annotations.toIsl(): IonElement {
    val optionality = when (this.defaultOptionality) {
        is IonSchemaModel.Optionality.Required -> "required"
        is IonSchemaModel.Optionality.Optional -> "optional"
        else -> null
    }
    val ordered = when {
        this.isOrdered.booleanValue -> "ordered"
        else -> null
    }

    return ionListOf(this.annos.items.map { it.toIsl() }, listOfNotNull(optionality, ordered))
}

private fun IonSchemaModel.Annotation.toIsl(): IonElement {
    val optionality = when (this.optionality) {
        is IonSchemaModel.Optionality.Required -> "required"
        is IonSchemaModel.Optionality.Optional -> "optional"
        else -> null
    }
    return ionSymbol(this.text.text, listOfNotNull(optionality))
}

private fun IonSchemaModel.ValidValuesSpec.toIsl(): IonElement =
    when (this) {
        is IonSchemaModel.ValidValuesSpec.OneOfValidValues -> ionListOf(this.values)
        is IonSchemaModel.ValidValuesSpec.RangeOfValidValues -> this.range.toIsl()
    }

private fun IonSchemaModel.OccursSpec.toIsl(): IonElement =
    when (this) {
        is IonSchemaModel.OccursSpec.OccursRule -> this.rule.toIsl()
        is IonSchemaModel.OccursSpec.OccursOptional -> ionSymbol("optional")
        is IonSchemaModel.OccursSpec.OccursRequired -> ionSymbol("required")
    }

private fun IonSchemaModel.TypeReference.toIsl(): IonElement {
    fun nullableAnnos(nullable: Boolean) =
        when {
            nullable -> listOf("nullable")
            else -> listOf()
        }

    return when (this) {
        is IonSchemaModel.TypeReference.NamedType -> ionSymbol(this.name.text, annotations = nullableAnnos(this.nullable.booleanValue))
        // TODO: since the type definition inside may contain a nullable type constraint, this feels redundant?
        // is that just a quirk of ISL or is it something we need to fix.
        is IonSchemaModel.TypeReference.InlineType -> this.type.toIsl(isInline = true).withAnnotations(nullableAnnos(this.nullable.booleanValue))
        is IonSchemaModel.TypeReference.ImportedType -> this.toIsl()
    }
}

private fun IonSchemaModel.ValuesRange.toIsl(): IonElement {
    return when (this) {
        is IonSchemaModel.ValuesRange.NumRange -> this.range.toIsl()
        is IonSchemaModel.ValuesRange.TimestampRange -> this.range.toIsl()
    }
}

private fun IonSchemaModel.TsValueRange.toIsl(): IonElement =
    ionListOf(this.min.toIsl(), this.max.toIsl(), annotations = listOf("range"))

private fun IonSchemaModel.TsValueExtent.toIsl(): IonElement =
    when (this) {
        is IonSchemaModel.TsValueExtent.MinTsValue -> MIN
        is IonSchemaModel.TsValueExtent.MaxTsValue -> MAX
        is IonSchemaModel.TsValueExtent.InclusiveTsValue -> this.value
        is IonSchemaModel.TsValueExtent.ExclusiveTsValue -> this.value.withAnnotations("exclusive")
    }

private fun IonSchemaModel.TypeReference.ImportedType.toIsl(): IonElement {
    val nullable = when {
        this.nullable.booleanValue -> "nullable"
        else -> null
    }
    val alias = when {
        this.alias != null -> listOf(field("as", ionSymbol(this.alias.text)))
        else -> listOf()
    }

    return ionStructOf(listOf(field("id", ionString(this.id.text)), field("type", ionSymbol(this.type.text))) + alias, annotations = listOfNotNull(nullable))
}

private fun IonSchemaModel.NumberRule.toIsl(): IonElement =
    when (this) {
        is IonSchemaModel.NumberRule.EqualsNumber -> value
        is IonSchemaModel.NumberRule.EqualsRange -> this.range.toIsl()
    }

private fun IonSchemaModel.NumberRange.toIsl(): IonElement =
    ionListOf(this.min.toIsl(), this.max.toIsl(), annotations = listOf("range"))

private fun IonSchemaModel.NumberExtent.toIsl(): IonElement =
    when (this) {
        is IonSchemaModel.NumberExtent.Min -> MIN
        is IonSchemaModel.NumberExtent.Max -> MAX
        is IonSchemaModel.NumberExtent.Inclusive -> this.value
        is IonSchemaModel.NumberExtent.Exclusive -> this.value.withAnnotations("exclusive")
    }
