package org.partiql.testscript.parser.ast.builders

import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonType
import org.partiql.testscript.parser.Error
import org.partiql.testscript.parser.Result
import org.partiql.testscript.parser.ScriptLocation
import org.partiql.testscript.parser.Success
import org.partiql.testscript.parser.UnexpectedFieldError
import org.partiql.testscript.parser.ast.AppendTestNode

internal class AppendTestBuilder(location: ScriptLocation) : StructBuilder<AppendTestNode>("append_test", location) {

    override fun build(): Result<AppendTestNode> {
        val pattern = fieldMap.remove("pattern")
        val additionalData = fieldMap.remove("additional_data")

        fieldMap.forEach { (k, v) -> errors.add(UnexpectedFieldError("$path.$k", v.scriptLocation)) }

        validateRequired("$path.pattern", pattern)
        validateType("$path.pattern", pattern, IonType.STRING)

        validateRequired("$path.additional_data", additionalData)
        validateType("$path.additional_data", additionalData, IonType.STRUCT)

        return if (errors.isEmpty()) {
            Success(AppendTestNode(
                    pattern = (pattern!!.ionValue as IonString).stringValue(),
                    additionalData = additionalData!!.ionValue as IonStruct,
                    scriptLocation = location))
        } else {
            Error(errors)
        }
    }
}