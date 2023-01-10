package org.partiql.annotations

/**
 * This is a "global" annotation class to mark a certain feature as experimental in the codebase
 *
 * To use features that are marked experimental, users are required to give explicit consent.
 *
 * To read more on the opt-in requirements, see https://kotlinlang.org/docs/opt-in-requirements.html#opt-in-to-using-api.
 */

@Deprecated("We are moving toward feature level Opt-in requirement. This opt-in requirement is not used anymore. Remove its usages from your code.")
@RequiresOptIn(message = "This feature is experimental. It may be changed in the future without notice.", level = RequiresOptIn.Level.ERROR)
annotation class PartiQLExperimental
