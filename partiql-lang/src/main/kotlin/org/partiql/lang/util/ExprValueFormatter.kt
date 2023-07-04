package org.partiql.lang.util

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.dateValue
import org.partiql.lang.eval.name
import org.partiql.lang.eval.partiQLTimestampValue
import org.partiql.lang.eval.timeValue
import org.partiql.lang.eval.toIonValue
import java.math.BigDecimal

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
        private val ion = IonSystemBuilder.standard().build()

        private var currentIndentation: Int = 0

        fun recursivePrettyPrint(value: ExprValue) {
            when (value.type) {
                ExprValueType.MISSING -> out.append(MISSING_STRING)
                ExprValueType.NULL -> out.append(NULL_STRING)
                ExprValueType.BOOL -> out.append(value.scalar.booleanValue().toString())
                ExprValueType.INT -> out.append(value.scalar.numberValue().toString())
                ExprValueType.DECIMAL -> {
                    val decimalValue = value.scalar.numberValue() as? BigDecimal
                    out.append(decimalValue.toString())
                    // If this is a decimal with scale to be 0, we need to add a decimal point to differentiate it from an integer
                    if (decimalValue != null && decimalValue.scale() == 0) {
                        out.append(".")
                    }
                }
                ExprValueType.STRING -> out.append("'${value.scalar.stringValue()}'")
                ExprValueType.DATE -> out.append("DATE '${value.dateValue()}'")
                ExprValueType.TIME -> {
                    val time = value.timeValue()
                    val prefix = if (time.offsetTime == null) "TIME" else "TIME WITH TIME ZONE"
                    out.append("$prefix '$time'")
                }
                ExprValueType.TIMESTAMP -> {
                    val timestamp = value.partiQLTimestampValue()
                    out.append("TIMESTAMP '${timestamp.toStringSQL()}'")
                }

                // fallback to an Ion literal for all types that don't have a native PartiQL representation
                ExprValueType.FLOAT, ExprValueType.SYMBOL,
                ExprValueType.CLOB, ExprValueType.BLOB, ExprValueType.SEXP -> prettyPrintIonLiteral(value)

                ExprValueType.LIST -> prettyPrintContainer(value, "[", "]")
                ExprValueType.BAG -> prettyPrintContainer(value, "<<", ">>")
                ExprValueType.STRUCT -> prettyPrintContainer(value, "{", "}") { v ->
                    val fieldName = v.name!!.scalar.stringValue()
                    out.append("'$fieldName': ")
                    recursivePrettyPrint(v)
                }
                ExprValueType.GRAPH -> {
                    val g = value.graphValue
                    out.append("graph{$g}")
                }
            }
        }

        private fun prettyPrintContainer(
            value: ExprValue,
            openingMarker: String,
            closingMarker: String,
            prettyPrintElement: (ExprValue) -> Unit = { v -> recursivePrettyPrint(v) }
        ) {

            val iterator = value.iterator()

            if (iterator.hasNext()) {
                out.append(openingMarker).append(config.containerValueSeparator)

                currentIndentation += 1

                val firstElement = iterator.next()
                writeIndentation()
                prettyPrintElement(firstElement)

                iterator.forEach { v ->
                    out.append(",")
                    if (config.containerValueSeparator.isEmpty()) {
                        out.append(" ")
                    } else {
                        out.append(config.containerValueSeparator)
                    }

                    writeIndentation()
                    prettyPrintElement(v)
                }

                currentIndentation -= 1

                out.append(config.containerValueSeparator)
                writeIndentation()
                out.append(closingMarker)
            } else {
                // empty container
                out.append(openingMarker).append(closingMarker)
            }
        }

        private fun prettyPrintIonLiteral(value: ExprValue) {
            val ionValue = value.toIonValue(ion)
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
