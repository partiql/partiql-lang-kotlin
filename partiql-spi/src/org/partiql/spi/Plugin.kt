package org.partiql.spi

import com.amazon.ion.IonSystem
import kotlin.reflect.KClass

interface Plugin {

    val ion: IonSystem

    fun getSplitSource(source: SourceHandle): SplitSource

    fun getRecordSource(split: Split): RecordSource

    interface Factory {
        val identifier: String
        val sourceResolver: SourceResolver
        val scalarLib: ScalarLib?
            get() = null

        val config: KClass<*>

        fun create(ion: IonSystem, config: Any?): Plugin
    }

    // likely pointless because inversion
    interface ScalarLib
}
