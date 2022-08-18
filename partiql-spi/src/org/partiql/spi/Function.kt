package org.partiql.spi

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ScalarFunction(
    val names: Array<String>,
    val description: String = "",
    val returns: String,
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
annotation class Type(val t: String)
