package org.partiql.cli

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.partiql.lang.eval.ExprValueType.*
import java.io.*
import java.nio.charset.*

private val charset = Charset.forName("UTF-8") 
private val indentation = "  ".toByteArray(charset)

private val newLine = "\n".toByteArray(charset)
private val missingBytes = "MISSING".toByteArray(charset)
private val nullBytes = "NULL".toByteArray(charset)

interface ExprValuePrettyPrinter {
    fun prettyPrint(value: ExprValue)
}

internal class NonConfigurableExprValuePrettyPrinter(private val output: OutputStream): ExprValuePrettyPrinter {
    private var currentIndentation: Int = 0

    override fun prettyPrint(value: ExprValue) {
        currentIndentation = 0

        recursivePrettyPrint(value)
    }

    private fun recursivePrettyPrint(value: ExprValue): Unit {
        when (value.type) {

            MISSING                                    -> output.write(missingBytes)
            NULL                                       -> output.write(nullBytes)

            BOOL                                       -> write(value.scalar.booleanValue().toString())

            INT, DECIMAL                               -> write(value.scalar.numberValue().toString())

            STRING                                     -> write("'${value.scalar.stringValue()}'")

            // fallback to an Ion literal for all types that don't have a native PartiQL representation
            FLOAT, TIMESTAMP, SYMBOL, CLOB, BLOB, SEXP -> prettyPrintIonLiteral(value)

            LIST                                       -> prettyPrintContainer(value, "[", "]")
            BAG                                        -> prettyPrintContainer(value, "<<", ">>")
            STRUCT                                     -> prettyPrintContainer(value, "{", "}") { v ->
                val fieldName = v.name!!.scalar.stringValue()
                write("'$fieldName': ")

                recursivePrettyPrint(v)
            }
        }
    }


    private fun prettyPrintContainer(value: ExprValue,
                                     openingMarker: String,
                                     closingMarker: String,
                                     prettyPrintElement: (ExprValue) -> Unit = { v -> recursivePrettyPrint(v) }) {
        
        val iterator = value.iterator()
        
        if(iterator.hasNext()){
            write("$openingMarker\n")

            currentIndentation += 1
            
            val firstElement = iterator.next()
            writeIndentation()
            prettyPrintElement(firstElement)

            iterator.forEach { v ->
                write(",\n")
                writeIndentation()
                prettyPrintElement(v)
            }
            
            currentIndentation -= 1

            output.write(newLine)
            writeIndentation()
            write(closingMarker)
        }
        else {
            // empty container
            write("$openingMarker$closingMarker")
        }
    }


    private fun prettyPrintIonLiteral(value: ExprValue) {
        val ionValue = value.ionValue
        write("`")
        IonTextWriterBuilder.standard().build(output).use { writer -> ionValue.writeTo(writer) }
        write("`")
    }

    private fun write(s: String) {
        output.write(s.toByteArray(charset))
    }

    private fun writeIndentation() {
        IntRange(1, currentIndentation).forEach { output.write(indentation) }
    }
}


