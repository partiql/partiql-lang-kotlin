package org.partiql.lang.eval.physical.window

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.LOCAL_VARIABLE)
@RequiresOptIn(message = "This API is experimental. It may be changed in the future without notice.", level = RequiresOptIn.Level.ERROR)
annotation class ExperimentalWindowFunc
