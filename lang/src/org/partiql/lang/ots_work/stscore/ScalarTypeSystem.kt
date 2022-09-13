package org.partiql.lang.ots_work.stscore

import org.partiql.lang.ots_work.interfaces.Plugin

/**
 * [plugin] is the plugin that a PartiQL scalar type system uses. For now, let's assume there is only one plugin existed in the type system.
 */
class ScalarTypeSystem(
    val plugin: Plugin
)
