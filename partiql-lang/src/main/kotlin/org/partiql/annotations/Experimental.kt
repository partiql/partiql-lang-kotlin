package org.partiql.annotations

/**
 * This file holds annotation classes to mark a certain feature as experimental in the codebase
 *
 * To use features that are marked experimental in Kotlin, users are required to give explicit consent.
 *
 * To read more on the opt-in requirements, see https://kotlinlang.org/docs/opt-in-requirements.html#opt-in-to-using-api.
 *
 * Unfortunately, the annotation does not work when calling the classes with experimental annotation from Java.
 *
 * This means the java users will not receive such enforced communication, and can use the experimental annotation without warning.
 *
 * See: https://github.com/partiql/partiql-lang-kotlin/issues/965
 */

@RequiresOptIn(message = "PartiQLCompilerPipeline is experimental. It may be changed in the future without notice.", level = RequiresOptIn.Level.ERROR)
annotation class ExperimentalPartiQLCompilerPipeline

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
@RequiresOptIn(message = "Window Function is experimental. It may be changed in the future without notice.", level = RequiresOptIn.Level.ERROR)
annotation class ExperimentalWindowFunctions

@RequiresOptIn(message = "PartiQLSchemaInferencer is experimental. It may be changed in the future without notice.", level = RequiresOptIn.Level.ERROR)
annotation class ExperimentalPartiQLSchemaInferencer
