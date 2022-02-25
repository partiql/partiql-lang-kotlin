package org.partiql.lang.types

/**
 * Type signature of a [Type].
 */
data class TypeSignature(
    val typeName: String,
    val typeParameters: List<TypeSignatureParameter> = listOf()
)

/**
 * Parameter kind for a [TypeSignatureParameter].
 */
enum class ParameterKind {
    TYPE,
    INT,
    LONG,
    STRING
}

/**
 * Represents parameter of a [TypeSignature]
 */
class TypeSignatureParameter private constructor(val kind: ParameterKind, val value: Any) {
    companion object {
        fun of(typeSignature: TypeSignature): TypeSignatureParameter {
            return TypeSignatureParameter(ParameterKind.TYPE, typeSignature)
        }

        fun of(longLiteral: Long): TypeSignatureParameter {
            return TypeSignatureParameter(ParameterKind.LONG, longLiteral)
        }

        fun of(integer: Int): TypeSignatureParameter {
            return TypeSignatureParameter(ParameterKind.INT, integer)
        }

        fun of(string: String): TypeSignatureParameter {
            return TypeSignatureParameter(ParameterKind.STRING, string)
        }
    }
}