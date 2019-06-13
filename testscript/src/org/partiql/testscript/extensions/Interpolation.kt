package org.partiql.testscript.extensions

import com.amazon.ion.*

class UndefinedVariableInterpolationException(val variableName: String) : RuntimeException()

private val regex = "\\$(\\w+)".toRegex()

internal fun IonValue.interpolate(variables: IonStruct): IonValue =
        when (this) {
            is IonSymbol -> {
                val symbolText = stringValue()

                val ionValue = if (symbolText.startsWith('$')) {
                    val variableName = symbolText.substring(1)
                    variables[variableName] ?: throw UndefinedVariableInterpolationException(variableName)
                } else {
                    this
                }
                
                ionValue.clone()
            }

            is IonString -> system.newString(stringValue().interpolate(variables))

            is IonList -> this.foldInterpolating(system.newEmptyList(), variables)

            is IonSexp -> {
                this.foldInterpolating(system.newEmptySexp(), variables)
            }

            is IonStruct -> this.fold(system.newEmptyStruct()) { struct, el ->
                struct.apply { add(el.fieldName, el.interpolate(variables)) }
            }

            else -> this.clone()

        }

private fun String.interpolate(struct: IonStruct): String {
    val buffer = StringBuilder()
    var lastOffset = 0

    regex.findAll(this).forEach { match ->
        val beforeVariableReference = this.substring(lastOffset, match.range.start)
        buffer.append(beforeVariableReference)

        val identifier = match.groups[1]!!.value
        val value = struct[identifier] ?: throw UndefinedVariableInterpolationException(identifier)

        val replaceValue = when (value) {
            is IonText -> value.stringValue()
            else -> value.toIonText()
        }
        buffer.append(replaceValue)

        lastOffset = match.range.endInclusive + 1
    }

    val trailingText = this.substring(lastOffset)
    buffer.append(trailingText)

    return buffer.toString()
}


private fun IonSequence.foldInterpolating(target: IonSequence, variables: IonStruct) =
        this.fold(target) { acc, el ->
            val interpolated = el.interpolate(variables)
            acc.add(interpolated)
            acc
        }
