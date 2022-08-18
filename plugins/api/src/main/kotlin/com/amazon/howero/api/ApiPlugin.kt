package com.amazon.howero.api

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.spi.Plugin
import org.partiql.spi.RecordSource
import org.partiql.spi.Source
import org.partiql.spi.SourceHandle
import org.partiql.spi.SourceResolver
import org.partiql.spi.Split
import org.partiql.spi.SplitSource
import kotlin.reflect.KClass

class ApiPlugin(override val ion: IonSystem) : Plugin {

    private val client = ApiClient()

    override fun getRecordSource(split: Split): RecordSource = ApiRecordSource(
        client = client,
        ion = ion,
        split = split as ApiSplit
    )

    override fun getSplitSource(source: SourceHandle): SplitSource = ApiSplitSource(
        split = ApiSplit((source as ApiSourceHandle).url)
    )

    class Factory : Plugin.Factory {

        override val identifier: String = "api"

        override val config: KClass<*> = Any::class

        override val sourceResolver: SourceResolver = Sources

        override fun create(ion: IonSystem, config: Any?): Plugin = ApiPlugin(ion)

    }

    object Sources : SourceResolver() {

        /**
         * SELECT * FROM api.load('url')
         */
        @Source
        fun load(url: String): SourceHandle = ApiSourceHandle(url)

    }

}

class ApiSourceHandle(val url: String) : SourceHandle

// pointless in this scenario
class ApiSplit(val url: String) : Split

class ApiSplitSource(private val split: Split) : SplitSource {

    private val splits = listOf(split).iterator()

    override fun hasNext(): Boolean = splits.hasNext()

    override fun next(): Split = splits.next()

    override fun close() {}

}

class ApiRecordSource(val client: ApiClient, val ion: IonSystem, val split: ApiSplit) : RecordSource {

    override fun get(): Sequence<IonValue> {
        val v = client.load(split.url, ion)
        return sequenceOf(v)
    }

    override fun close() {}
}
