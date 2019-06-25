package org.partiql.testscript.parser

import com.amazon.ion.IonReader
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue

internal data class IonValueWithLocation(val ionValue: IonValue, val scriptLocation: ScriptLocation)

/**
 * A decorated IonReader specialized for the [Parser]
 */
internal class IonInputReader(val inputName: String,
                              private val ion: IonSystem,
                              private val reader: IonReader) : IonReader by reader {

    /**
     * SpanProvider does not provide accurate line numbers for structs, see https://github.com/amzn/ion-java/issues/226
     * and https://github.com/amzn/ion-java/issues/233
     *
     * This methods uses reflection to extract a reliable line number, however it's incredibly hacky as it relies on
     * accessing IonReader implementation private fields and can break at any time.
     */
    fun currentScriptLocation(): ScriptLocation {
        val lineNumber: Long = try {
            val scannerField = reader.javaClass.superclass.superclass.getDeclaredField("_scanner")
            scannerField.isAccessible = true
            val scanner = scannerField.get(reader)

            val lineCountField = scanner.javaClass.getDeclaredField("_line_count")
            lineCountField.isAccessible = true

            lineCountField.get(scanner) as Long
        } catch (_: NoSuchFieldException) {
            -1
        }

        return ScriptLocation(inputName, lineNumber)
    }

    fun ionValueWithLocation(): IonValueWithLocation {
        val location = currentScriptLocation()
        
        return IonValueWithLocation(ion.newValue(reader), location)
    }
    
    fun stepIn(block: (Sequence<IonInputReader>) -> Unit) {
        this.stepIn()
        block(this.asSequence())
        this.stepOut()
    }

    fun asSequence(): Sequence<IonInputReader> = Sequence {
        object: Iterator<IonInputReader> {
            var nextCalled = false
            var hasNext = false
            
            private fun handleHasNext() {
                if(!nextCalled) {
                    hasNext = this@IonInputReader.next() != null
                    nextCalled = true
                }
            }
            
            override fun next(): IonInputReader {
                handleHasNext()

                nextCalled = false
                
                return this@IonInputReader
            }

            override fun hasNext(): Boolean {
                handleHasNext()
                
                return hasNext
            }
        }
    }
}
