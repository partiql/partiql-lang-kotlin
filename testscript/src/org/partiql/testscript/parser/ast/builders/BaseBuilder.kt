package org.partiql.testscript.parser.ast.builders

import com.amazon.ion.IonType
import org.partiql.testscript.Result
import org.partiql.testscript.parser.*

internal abstract class BaseBuilder<T>(val location: ScriptLocation) {
    protected val errors = mutableListOf<ParserError>()

    protected fun validateRequired(label: String, field: Any?) {
        if (field == null) {
            errors.add(MissingRequiredError(label, location))
        }
    }
    
    
    
    abstract fun build(): Result<T>
}

internal abstract class StructBuilder<T>(val path: String, location: ScriptLocation) : BaseBuilder<T>(location) {
    protected val fieldMap = mutableMapOf<String, IonValueWithLocation>()

    protected fun validateType(label: String, field: IonValueWithLocation?, expected: IonType) {
        if (field != null && field.ionValue.type != expected) {
            errors.add(UnexpectedIonTypeError(label, expected, field.ionValue.type, field.scriptLocation))
        }
    }

    /**
     * Must run after extracting known fields
     */
    protected fun validateUnexpectedFields() {
        fieldMap.forEach { (k, v) -> errors.add(UnexpectedFieldError("$path.$k", v.scriptLocation)) }
    }

    fun setValue(name: String, value: IonValueWithLocation) {
        if (fieldMap.containsKey(name)) {
            errors.add(DuplicatedFieldError("$path.$name", location))
        } else {
            fieldMap[name] = value
        }
    }
}
