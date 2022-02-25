package org.partiql.lang.types

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.stringValue
import kotlin.reflect.KClass

val valueFactory = ExprValueFactory.standard(IonSystemBuilder.standard().build())

/**
 * An example implementation of a RedShift's VARCHAR type.
 * TODO: Remove this file or move this to a test location.
 */
data class RSVarcharType(val length: Int): Type {

    override val typeSignature: TypeSignature
        get() = TypeSignature("RS_VARCHAR", listOf(TypeSignatureParameter.of(Integer.MAX_VALUE)))

    override fun getDisplayName(): String {
        return "RS_VARCHAR"
    }

    override fun writeExprValue(value: Any): ExprValue {
        value as String
        return valueFactory.newString(value)
    }

    override fun readExprValue(value: ExprValue): Any {
        return value.stringValue()
    }

    override fun cast(value: ExprValue): ExprValue {
        TODO("Not yet implemented")
    }

    override fun matches(value: ExprValue): Boolean {
        TODO("Not yet implemented")
    }

}

/**
 * An example implementation of RedShift plugin registering RS_VARCHAR type and RS_CONCAT function.
 */
class RSPlugin : Plugin {
    override fun getTypes(): Map<String, KClass<*>> {
        return mapOf("RS_VARCHAR" to RSVarcharType::class)
    }

    override fun getFunctions(): List<SymbolTableEntry> {
        val rsTypeSignature = TypeSignature(
            "RS_VARCHAR",
            listOf(TypeSignatureParameter.of(Integer.MAX_VALUE))
        )
        val rsConcat = object: SymbolTableEntry {
            override val funName: String
                get() = "RS_CONCAT"

            override val argTypes: List<Type>
                get() = listOf(
                    TypeAndFunctionManager.getType(rsTypeSignature),
                    TypeAndFunctionManager.getType(rsTypeSignature)
                )

            override fun invoke(exprValues: List<ExprValue>): ExprValue {
                assert(exprValues.size == 2)
                val str1 = exprValues[0].stringValue()
                val str2 = exprValues[1].stringValue()
                return valueFactory.newString(str1 + str2)
            }
        }
        return listOf(rsConcat)
    }
}