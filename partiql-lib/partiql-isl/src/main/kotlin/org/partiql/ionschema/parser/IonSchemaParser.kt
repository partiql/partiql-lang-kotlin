package org.partiql.ionschema.parser

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonElementConstraintException
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.TimestampElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionSymbol
import org.partiql.ionschema.model.IonSchemaModel
import org.partiql.pig.runtime.SymbolPrimitive

private val MIN = ionSymbol("min")
private val MAX = ionSymbol("max")
internal val validAnnotationsForAnnotationsConstraint = setOf("ordered", "required", "optional")
internal val validAnnotationsForAnnotationConstraint = setOf("required", "optional")
internal val validAnnotationsForTypeReference = setOf("nullable", "type")
internal val timestampOffsetPatternRegex = Regex("[+-]\\d\\d:\\d\\d")
internal val validImportedTypeFields = listOf("id", "type", "as")

/**
 * Transforms an ISL document into an [IonSchemaModel.Schema], which is a PIG-generated in-memory object representing
 * ISL entities.
 *
 * @param elements an [IonElement] representation of an ISL document to be transformed to [IonSchemaModel.Schema]
 * @return [elements] transformed into an [IonSchemaModel.Schema]
 */
fun parseSchema(elements: List<AnyElement>): IonSchemaModel.Schema =
    try {
        IonSchemaModel.build {
            // Mutation is icky--a solution that doesn't utilize mutation would be better.
            var hasHeader = false
            var hasFooter = false
            var hasType = false

            val stmts = elements.map {
                when {
                    it.annotations.contains("type") -> {
                        hasType = true

                        if (hasFooter) {
                            parseError(it, Error.TypeNotAllowedAfterFooter)
                        }

                        typeStatement(parseTypeDefinition(it.asStruct(), isInline = false))
                    }
                    it.annotations.contains("schema_header") -> {
                        if (hasHeader) {
                            parseError(it, Error.MoreThanOneHeaderFound)
                        }
                        if (hasType) {
                            parseError(it, Error.HeaderMustAppearBeforeTypes)
                        }
                        hasHeader = true
                        parseHeader(it.asStruct())
                    }
                    it.annotations.contains("schema_footer") -> {
                        if (hasFooter) {
                            parseError(it, Error.MoreThanOneFooterFound)
                        }
                        if (!hasHeader) {
                            parseError(it, Error.FooterMustAppearAfterHeader)
                        }
                        hasFooter = true

                        parseFooter(it.asStruct())
                    }
                    else -> contentStatement(it)
                }
            }

            if (hasHeader && !hasFooter) {
                parseError(
                    elements.first { it.annotations.contains("schema_header") },
                    Error.HeaderPresentButNoFooter
                )
            }

            schema(stmts).also { validateSchemaModel(it) }
        }
    } catch (ex: IonElementConstraintException) {
        parseError(
            ex.location,
            Error.IonElementConstraintException(ex.message ?: "<exception message was null>")
        )
    }

private fun parseHeader(elem: StructElement): IonSchemaModel.SchemaStatement.HeaderStatement {
    return extractAllFields(elem) {
        val importList = extractOptional("imports") { parseImportList(it.asList()) }
        val openFields = extractRemainingFields().map { IonSchemaModel.build { openField(it.name, it.value) } }

        IonSchemaModel.build {
            headerStatement(
                imports = importList,
                openContent = openFieldList(openFields)
            )
        }
    }
}

private fun parseImportList(listElem: ListElement): IonSchemaModel.ImportList {
    val imports: List<IonSchemaModel.Import> = listElem.values.map { listItem ->
        extractAllFields(listItem.asStruct()) {
            val id = extractRequired("id") { it.textValue }
            val type = extractOptional("type") { it.symbolValue }
            val asAlias = extractOptional("as") { it.symbolValue }

            if (asAlias != null && type == null) {
                parseError(listItem, Error.ImportMissingTypeFieldWhenAsSpecified)
            }

            IonSchemaModel.build { import(id, type, asAlias) }
        }
    }
    return IonSchemaModel.build { importList(imports) }
}

private fun parseFooter(elem: StructElement): IonSchemaModel.SchemaStatement.FooterStatement =
    IonSchemaModel.build {
        footerStatement(openFieldList(elem.fields.map { openField(it.name, it.value) }))
    }

private typealias ConstraintParselet = IonSchemaModel.Builder.(AnyElement) -> IonSchemaModel.Constraint

private fun constraintParselet(name: String, block: ConstraintParselet): Pair<String, ConstraintParselet> = Pair(name, block)

private val constraintParselets = mapOf(
    constraintParselet("content") { it: AnyElement ->
        if (it.symbolValue != "closed") {
            parseError(it, Error.ValueOfClosedFieldNotContentSymbol(it))
        }
        closedContent()
    },
    constraintParselet("type") { it: AnyElement ->
        typeConstraint(parseTypeReference(it))
    },
    constraintParselet("codepoint_length") {
        codepointLength(parseNumberRule(it))
    },
    constraintParselet("precision") {
        precision(parseNumberRule(it))
    },
    constraintParselet("scale") {
        scale(parseNumberRule(it))
    },
    constraintParselet("element") {
        element(parseTypeReference(it))
    },
    constraintParselet("byte_length") {
        byteLength(parseNumberRule(it))
    },
    constraintParselet("container_length") {
        containerLength(parseNumberRule(it))
    },
    constraintParselet("regex") {
        parseRegexConstraint(it.asString())
    },
    constraintParselet("not") {
        not(parseTypeReference(it))
    },
    constraintParselet("all_of") {
        allOf(parseTypeReferenceList(it))
    },
    constraintParselet("any_of") {
        anyOf(parseTypeReferenceList(it))
    },
    constraintParselet("one_of") {
        oneOf(parseTypeReferenceList(it))
    },
    constraintParselet("ordered_elements") {
        orderedElements(parseTypeReferenceList(it))
    },
    constraintParselet("contains") {
        contains(it.listValues)
    },
    constraintParselet("occurs") { it ->
        occurs(
            when {
                it is SymbolElement -> {
                    when (it.textValue) {
                        "optional" -> this.occursOptional()
                        "required" -> this.occursRequired()
                        else -> parseError(it, Error.InvalidOccursSpec(it))
                    }
                }
                it is ListElement || it.isNumber -> {
                    occursRule(parseNumberRule(it))
                }
                else -> parseError(it, Error.InvalidOccursSpec(it))
            }
        )
    },
    constraintParselet("valid_values") { it ->
        validValues(
            when {
                it.annotations.contains("range") -> rangeOfValidValues(parseValuesRange(it))
                it is ListElement -> oneOfValidValues(it.listValues)
                else -> parseError(it, Error.InvalidValidValuesSpec(it))
            }
        )
    },
    constraintParselet("fields") { elem ->
        elem.requireZeroAnnotations()

        val structElem = elem.asStruct()

        IonSchemaModel.build {
            fields(
                structElem.fields.map { structField ->
                    field(structField.name, parseTypeReference(structField.value))
                }
            )
        }
    },
    constraintParselet("annotations") {
        parseAnnotations(it)
    },
    constraintParselet("timestamp_precision") {
        timestampPrecision(parseTimestampPrecision(it))
    },
    constraintParselet("timestamp_offset") {
        timestampOffset(parseTimestampOffset(it))
    },
    constraintParselet("utf8_byte_length") {
        utf8ByteLength(parseNumberRule(it))
    }
)

internal fun parseAnnotations(value: AnyElement): IonSchemaModel.Constraint.Annotations {
    val annotationsList = value.asList().requireUniqueAnnotations()

    if (annotationsList.annotations.size !in 0..2) {
        parseError(annotationsList, Error.UnexpectedAnnotationCount(0..2, annotationsList.annotations.size))
    }

    annotationsList.annotations.forEach {
        if (it !in validAnnotationsForAnnotationsConstraint) {
            parseError(annotationsList, Error.InvalidAnnotationsForAnnotationsConstraint(it))
        }
    }

    if (annotationsList.annotations.containsAll(validAnnotationsForAnnotationConstraint)) {
        parseError(annotationsList, Error.CannotIncludeRequiredAndOptional)
    }

    val optionality = when {
        annotationsList.annotations.contains("required") -> IonSchemaModel.build { required() }
        annotationsList.annotations.contains("optional") -> IonSchemaModel.build { optional() }
        else -> null
    }

    val isOrdered = when {
        annotationsList.annotations.contains("ordered") -> ionBool(true)
        else -> ionBool(false)
    }

    val annos = annotationsList.values.map {
        it.allowSingleAnnotation(validAnnotationsForAnnotationConstraint)
        val anno = it.annotations.firstOrNull()
        IonSchemaModel.build {
            annotation(
                it.textValue,
                when (anno) {
                    "required" -> IonSchemaModel.build { required() }
                    "optional" -> IonSchemaModel.build { optional() }
                    null -> null
                    else -> parseError(it, Error.AnnotationNotAllowedHere(anno))
                }
            )
        }
    }

    return IonSchemaModel.build { annotations(isOrdered, annotationList(annos), optionality) }
}

internal fun parseTimestampPrecision(tsp: AnyElement): IonSchemaModel.TsPrecision = IonSchemaModel.build {
    when {
        tsp.allowSingleAnnotation("range") -> equalsTsPrecisionRange(parseTsPrecisionRange(tsp))
        else -> equalsTsPrecisionValue(parseTsPrecisionValue(tsp))
    }
}

internal fun parseValuesRange(valuesRange: AnyElement): IonSchemaModel.ValuesRange {
    val listElem = valuesRange
        .requireSingleAnnotation("range")
        .asList()
        .requireSize(2)

    val fromElem = listElem.values[0]
    val toElem = listElem.values[1]

    return when {
        fromElem.isNumber || toElem.isNumber -> IonSchemaModel.build { numRange(parseNumberRange(valuesRange)) }
        fromElem is TimestampElement || toElem is TimestampElement -> parseTimestampValuesRange(valuesRange)
        else -> parseError(valuesRange, Error.InvalidValidValuesRangeExtent)
    }
}

internal fun parseTimestampValuesRange(tsValueRange: AnyElement): IonSchemaModel.ValuesRange.TimestampRange {
    val listElem = tsValueRange
        .requireSingleAnnotation("range")
        .asList()
        .requireSize(2)

    val fromElem = listElem.values[0]
    val toElem = listElem.values[1]

    return IonSchemaModel.build {
        timestampRange(
            tsValueRange(
                parseTimestampValuesExtent(fromElem),
                parseTimestampValuesExtent(toElem)
            )
        )
    }
}

internal fun parseTimestampValuesExtent(elem: AnyElement) =
    when (elem) {
        is SymbolElement -> when (elem) {
            MIN -> IonSchemaModel.build { minTsValue() }
            MAX -> IonSchemaModel.build { maxTsValue() }
            else -> parseError(elem, Error.InvalidTimestampExtent)
        }
        is TimestampElement -> if (elem.allowSingleAnnotation("exclusive")) {
            IonSchemaModel.build { exclusiveTsValue((elem as TimestampElement).withoutAnnotations()) }
        } else {
            IonSchemaModel.build { inclusiveTsValue(elem) }
        }
        else -> {
            parseError(elem, Error.InvalidTimestampExtent)
        }
    }

internal fun parseTsPrecisionRange(tspRange: AnyElement): IonSchemaModel.TsPrecisionRange {
    val tspRangeList = tspRange
        .asList()
        .requireSize(2)

    return IonSchemaModel.build { tsPrecisionRange(min = parseTsPrecisionExtent(tspRangeList.values[0]), max = parseTsPrecisionExtent(tspRangeList.values[1])) }
}

internal fun parseTsPrecisionExtent(tspExtent: AnyElement): IonSchemaModel.TsPrecisionExtent {
    if (tspExtent.textValue == "min") {
        return IonSchemaModel.build { minTsp() }
    }
    if (tspExtent.textValue == "max") {
        return IonSchemaModel.build { maxTsp() }
    }

    return when {
        tspExtent.allowSingleAnnotation("exclusive") -> IonSchemaModel.build { exclusiveTsp(parseTsPrecisionValue(tspExtent)) }
        else -> IonSchemaModel.build { inclusiveTsp(parseTsPrecisionValue(tspExtent)) }
    }
}

internal fun parseTsPrecisionValue(precision: AnyElement): IonSchemaModel.TsPrecisionValue =
    when (precision.textValue) {
        "year" -> IonSchemaModel.build { year() }
        "month" -> IonSchemaModel.build { month() }
        "day" -> IonSchemaModel.build { day() }
        "minute" -> IonSchemaModel.build { minute() }
        "second" -> IonSchemaModel.build { second() }
        "millisecond" -> IonSchemaModel.build { millisecond() }
        "microsecond" -> IonSchemaModel.build { microsecond() }
        "nanosecond" -> IonSchemaModel.build { nanosecond() }
        else -> parseError(precision, Error.InvalidTimeStampPrecision(precision.textValue))
    }

internal fun parseTimestampOffset(offset: AnyElement): List<String> {
    return offset
        .requireZeroAnnotations()
        .asList()
        .requireNonzeroListSize()
        .values.map { parseTimestampOffsetPattern(it.asString()) }
}

internal fun parseTimestampOffsetPattern(offsetPattern: StringElement): String {
    if (!offsetPattern.textValue.matches(timestampOffsetPatternRegex)) {
        parseError(offsetPattern, Error.InvalidTimeStampOffsetPattern(offsetPattern.textValue))
    }

    if (offsetPattern.textValue.substring(1, 3).toInt() !in 0..23) {
        parseError(offsetPattern, Error.InvalidTimeStampOffsetValueForHH(offsetPattern.textValue))
    }

    if (offsetPattern.textValue.substring(4, 6).toInt() !in 0..59) {
        parseError(offsetPattern, Error.InvalidTimeStampOffsetValueForMM(offsetPattern.textValue))
    }

    return offsetPattern.textValue
}

private fun parseRegexConstraint(regex: StringElement): IonSchemaModel.Constraint {
    if (regex.annotations.size !in 0..2) {
        parseError(regex, Error.UnexpectedAnnotationCount(0..2, regex.annotations.size))
    }

    val invalidAnno = regex.annotations.firstOrNull() { it != "i" && it != "m" }
    if (invalidAnno != null) {
        parseError(regex, Error.UnexpectedAnnotation(invalidAnno))
    }

    if (regex.annotations.size == 2 && regex.annotations[0] == "m") {
        parseError(regex, Error.IncorrectRegexPropertyOrder)
    }

    val hasI = regex.annotations.contains("i")
    val hasM = regex.annotations.contains("m")
    return IonSchemaModel.build {
        regex(regex.textValue, caseInsensitive = ionBool(hasI), multiline = ionBool(hasM))
    }
}

/**
 * Transforms an ISL type definition into an [IonSchemaModel.TypeDefinition].
 *
 * @param struct an [IonElement] representation of an ISL type definition to be transformed to
 *  [IonSchemaModel.TypeDefinition]
 * @param isInline indicates if [struct] is an inline type. If false, checks that [struct] has the "type" annotation
 * @return [struct] transformed into an [IonSchemaModel.TypeDefinition]
 * @throws [IonSchemaParseException] if [isInline] is false and the "type" annotation is not included in [struct]
 */
fun parseTypeDefinition(struct: StructElement, isInline: Boolean): IonSchemaModel.TypeDefinition {
    if (!isInline) {
        struct.requireSingleAnnotation("type")
    }

    return extractAllFields(struct) {
        val typeName = extractOptional("name") { it.asSymbol() }

        val constraintElements = extractRemainingFields()
        // TODO:  support open content.

        IonSchemaModel.build {
            val constraints = constraintElements.map {
                // TODO: allow open content here, remove the parseError!
                val parselet = constraintParselets[it.name]
                when (parselet) {
                    null -> arbitraryConstraint(name = it.name, value = it.value)
                    else -> parselet(it.value)
                }
            }

            typeDefinition_(
                name = typeName?.toSymbolPrimitive(),
                constraints = constraintList(constraints)
            )
        }
    }
}

private fun parseTypeReferenceList(elem: AnyElement): List<IonSchemaModel.TypeReference> {
    val listElem = elem.asList()
    return listElem.values.map { parseTypeReference(it) }
}

internal fun parseTypeReference(elem: AnyElement): IonSchemaModel.TypeReference {
    elem.allowAnnotations(validAnnotationsForTypeReference)
    val isNullable = ionBool(elem.annotations.contains("nullable"))
    return IonSchemaModel.build {
        when (elem) {
            is SymbolElement -> namedType(elem.symbolValue, isNullable)
            is StructElement -> when {
                elem.getOptional("id") != null -> parseImportedType(elem)
                else -> inlineType(parseTypeDefinition(elem, isInline = true), isNullable)
            }
            else -> parseError(elem, Error.TypeReferenceMustBeSymbolOrStruct)
        }
    }
}

internal fun parseImportedType(elem: StructElement): IonSchemaModel.TypeReference.ImportedType {
    if (elem.fields.size !in 2..3) {
        parseError(elem, Error.UnexpectedNumberOfFields(2..3, elem.fields.size))
    }

    val invalidFields = elem.fields.map { it.name } - validImportedTypeFields
    if (invalidFields.isNotEmpty()) {
        parseError(elem, Error.InvalidFieldsForInlineImport(invalidFields))
    }

    return IonSchemaModel.build {
        importedType(
            id = elem["id"].textValue,
            type = elem["type"].textValue,
            nullable = if (elem.annotations.contains("nullable")) ionBool(true) else ionBool(false),
            alias = elem.getOptional("as")?.textValue
        )
    }
}

internal fun parseNumberRule(elem: AnyElement): IonSchemaModel.NumberRule =
    if (elem.isNumber) {
        elem.requireZeroAnnotations()
        IonSchemaModel.build {
            equalsNumber(elem)
        }
    } else {
        IonSchemaModel.build {
            equalsRange(parseNumberRange(elem))
        }
    }

private val IonElement.isNumber get() = NUMBER_TYPES.contains(this.type)

private fun parseNumberRange(elem: AnyElement): IonSchemaModel.NumberRange {
    val listElem = elem
        .requireSingleAnnotation("range")
        .asList()
        .requireSize(2)

    val fromElem = listElem.values[0]
    val toElem = listElem.values[1]

    return IonSchemaModel.build {
        numberRange(parseNumberExtent(fromElem), parseNumberExtent(toElem))
    }
}

private fun parseNumberExtent(elem: IonElement): IonSchemaModel.NumberExtent = IonSchemaModel.build {
    when {
        elem is SymbolElement ->
            when (elem) {
                MIN -> min()
                MAX -> max()
                else -> parseError(elem, Error.InvalidNumericExtent)
            }
        elem.isNumber ->
            if (elem.allowSingleAnnotation("exclusive")) {
                exclusive(elem.withoutAnnotations())
            } else {
                inclusive(elem)
            }
        else -> {
            parseError(elem, Error.InvalidNumericExtent)
        }
    }
}

private fun SymbolElement.toSymbolPrimitive() =
    SymbolPrimitive(this.textValue, this.metas)
