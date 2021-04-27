package org.partiql.lang.util

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.partiql.lang.eval.ExprValueType.*
import java.lang.StringBuilder
import java.time.LocalTime
import java.time.OffsetTime
import java.time.format.DateTimeFormatter

private const val MISSING_STRING = "MISSING"
private const val NULL_STRING = "NULL"

interface ExprValueFormatter {
    fun formatTo(value: ExprValue, out: Appendable)
    fun format(value: ExprValue): String {
        val sb = StringBuilder()
        formatTo(value, sb)
        return sb.toString()
    }
}

class ConfigurableExprValueFormatter(private val config: Configuration) : ExprValueFormatter {
    companion object {
        @JvmStatic
        val pretty: ConfigurableExprValueFormatter = ConfigurableExprValueFormatter(Configuration("  ", "\n"))

        @JvmStatic
        val standard: ConfigurableExprValueFormatter = ConfigurableExprValueFormatter(Configuration("", ""))
    }

    data class Configuration(val indentation: String, val containerValueSeparator: String)

    override fun formatTo(value: ExprValue, out: Appendable) {
        PrettyFormatter(out, config).recursivePrettyPrint(value)
    }

    private class PrettyFormatter(val out: Appendable, val config: Configuration) {
        private var currentIndentation: Int = 0

        fun recursivePrettyPrint(value: ExprValue) {
            when (value.type) {

                MISSING                                    -> out.append(MISSING_STRING)
                NULL                                       -> out.append(NULL_STRING)

                BOOL                                       -> out.append(value.scalar.booleanValue().toString())

                INT, DECIMAL                               -> out.append(value.scalar.numberValue().toString())

                STRING                                     -> out.append("'${value.scalar.stringValue()}'")

                DATE                                       -> out.append(value.scalar.dateValue().toString())

                TIME                                       -> out.append(value.scalar.timeValue().toString())

                // fallback to an Ion literal for all types that don't have a native PartiQL representation
                FLOAT, TIMESTAMP, SYMBOL, CLOB, BLOB, SEXP -> prettyPrintIonLiteral(value)

                LIST                                       -> prettyPrintContainer(value, "[", "]")
                BAG                                        -> prettyPrintContainer(value, "<<", ">>")
                STRUCT                                     -> prettyPrintContainer(value, "{", "}") { v ->
                    val fieldName = v.name!!.scalar.stringValue()
                    out.append("'$fieldName': ")

                    recursivePrettyPrint(v)
                }
            }
        }


        private fun prettyPrintContainer(value: ExprValue,
                                         openingMarker: String,
                                         closingMarker: String,
                                         prettyPrintElement: (ExprValue) -> Unit = { v -> recursivePrettyPrint(v) }) {

            val iterator = value.iterator()

            if (iterator.hasNext()) {
                out.append(openingMarker).append(config.containerValueSeparator)

                currentIndentation += 1

                val firstElement = iterator.next()
                writeIndentation()
                prettyPrintElement(firstElement)

                iterator.forEach { v ->
                    out.append(",")
                    if(config.containerValueSeparator.isEmpty()) {
                        out.append(" ")
                    }
                    else {
                        out.append(config.containerValueSeparator)
                    }

                    writeIndentation()
                    prettyPrintElement(v)
                }

                currentIndentation -= 1

                out.append(config.containerValueSeparator)
                writeIndentation()
                out.append(closingMarker)
            }
            else {
                // empty container
                out.append(openingMarker).append(closingMarker)
            }
        }


        private fun prettyPrintIonLiteral(value: ExprValue) {
            val ionValue = value.ionValue
            out.append("`")

            // We intentionally do *not* want to call [IonWriter.close()] on the [IonWriter] here because
            // that will also call 'out.close()`, which is bad because we probably have more stuff to write!
            ionValue.writeTo(IonTextWriterBuilder.standard().build(out))
            out.append("`")
        }

        private fun writeIndentation() {
            if (config.indentation.isNotEmpty()) {
                IntRange(1, currentIndentation).forEach { _ -> out.append(config.indentation) }
            }
        }
    }
}
