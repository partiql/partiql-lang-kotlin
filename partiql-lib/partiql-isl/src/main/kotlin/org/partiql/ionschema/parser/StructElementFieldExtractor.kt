package org.partiql.ionschema.parser

import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.StructField

// TODO: how should this class handle duplicate required or optional fields?
//  If we only call extract* once, the other fields of the same name will still be present
//  and will be returned from extractRemainingFields.
internal class StructElementFieldExtractor(val struct: StructElement) {
    private val remainingFields = struct.fields.toMutableList()

    val fieldsRemainingCount get() = remainingFields.size

    fun <T> extractOptional(fieldName: String, process: (AnyElement) -> T): T? {
        val matchingFields = remainingFields.filter { it.name == fieldName }
        return when (matchingFields.size) {
            0 -> null
            1 -> {
                val field = matchingFields.first()
                process(field.value).also {
                    remainingFields.remove(field)
                }
            }
            else -> parseError(struct, Error.DuplicateField(fieldName))
        }
    }

    fun <T> extractRequired(fieldName: String, process: (AnyElement) -> T): T =
        extractOptional(fieldName, process) ?: parseError(struct, Error.RequiredFieldMissing(fieldName))

    fun extractRemainingFields(): Iterable<StructField> {
        val leftovers = remainingFields.toList()
        remainingFields.clear()
        return leftovers
    }
}

internal fun <T> extractAllFields(struct: StructElement, block: StructElementFieldExtractor.() -> T): T {
    val extractor = StructElementFieldExtractor(struct)
    val extracted = extractor.block()

    if (extractor.fieldsRemainingCount > 0) {
        val unexpectedField = extractor.extractRemainingFields().first()
        parseError(unexpectedField.value, Error.UnexpectedField(unexpectedField.name))
    }

    return extracted
}
