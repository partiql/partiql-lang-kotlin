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

        val config: KClass<*>

        fun create(ion: IonSystem, config: Any?): Plugin

        fun scalarLib(ion: IonSystem): ScalarLib? = null
    }

    interface ScalarLib {
        val ion: IonSystem
    }
}
