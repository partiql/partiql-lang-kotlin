package org.partiql.spi

import com.amazon.ionelement.api.AnyElement

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Source

interface SourceHandle

abstract class SourceResolver {

    fun get(identifier: String, args: List<AnyElement>): SourceHandle {
        outer@ for (method in this::class.java.methods) {
            if (!method.isAnnotationPresent(Source::class.java)) {
                continue
            }
            val fName = identifier.toUpperCase()
            val mName = method.name.toUpperCase()
            if (fName != mName) {
                continue
            }
            if (args.size != method.parameterCount) {
                continue
            }
            // found a match, coerce arguments
            val types = method.parameterTypes
            val arguments = args.mapIndexed { i, arg ->
                when (val t = types[i]) {
                    Long::class.java -> arg.longValue
                    Double::class.java -> arg.doubleValue
                    String::class.java -> arg.textValue
                    Boolean::class.java -> arg.booleanValue
                    else -> throw IllegalArgumentException("type does $t does not map to Ion AnyElement type")
                }
            }.toTypedArray()
            return (method.invoke(this, *arguments) as SourceHandle)
        }
        throw IllegalArgumentException("no source function $identifier with signature matching ${args.joinToString()}")
    }
}
