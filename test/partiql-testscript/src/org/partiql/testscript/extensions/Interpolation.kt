package org.partiql.testscript.extensions

import com.amazon.ion.IonList
import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonText
import com.amazon.ion.IonValue

internal class UndefinedVariableInterpolationException(val variableName: String) : RuntimeException()

private val regex = "\\$([\\w{}]+)".toRegex()

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

private fun IonSequence.foldInterpolating(target: IonSequence, variables: IonStruct) =
    this.fold(target) { acc, el ->
        acc.add(el.interpolate(variables))
        acc
    }

private fun String.interpolate(variables: IonStruct): String {
    val matches = regex.findAll(this).map { it.groups[1]!!.value }

    return matches.fold(this) { interpolated, match ->

        val variableName = if (match.startsWith("{")) {
            match.trim { c -> c == '{' || c == '}' || c.isWhitespace() }
        } else {
            match
        }

        val replacement = variables[variableName]?.stringfy()
            ?: throw UndefinedVariableInterpolationException(variableName)
        interpolated.replace("\$$match", replacement)
    }
}

private fun IonValue.stringfy(): String = when (this) {
    is IonText -> this.stringValue() // to remove the extra "
    else -> this.toIonText()
}
