package com.amazon.howero.example

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.spi.Plugin
import org.partiql.spi.RecordSource
import org.partiql.spi.ScalarFunction
import org.partiql.spi.Source
import org.partiql.spi.SourceHandle
import org.partiql.spi.SourceResolver
import org.partiql.spi.Split
import org.partiql.spi.SplitSource
import org.partiql.spi.Type
import kotlin.reflect.KClass

class ExamplePlugin(override val ion: IonSystem) : Plugin {

    override fun getRecordSource(split: Split): RecordSource = object : RecordSource {

        override fun get(): Sequence<IonValue> = sequence {
            val s = ion.newEmptyStruct()
            val message = (split as ExampleSplit).message
            s.add("message", ion.newString(message))
            yield(s)
        }

    }

    override fun getSplitSource(source: SourceHandle) = object : SplitSource {

        private val splits = (1..(source as ExampleSource).times)
            .map { ExampleSplit(source.message) }
            .iterator()

        override fun hasNext(): Boolean = splits.hasNext()

        override fun next(): Split = splits.next()

    }

    class Factory : Plugin.Factory {

        override val identifier: String = "example"

        override val config: KClass<*> = Any::class

        override val sourceResolver: SourceResolver = object : SourceResolver() {

            /**
             * SELECT * FROM example.echo('message', times)
             */
            @Source
            fun echo(message: String, times: Long): SourceHandle = ExampleSource(message, times)

        }

        override fun create(ion: IonSystem, config: Any?): Plugin = ExamplePlugin(ion)

        override fun scalarLib(ion: IonSystem): Plugin.ScalarLib = ExampleScalarLib(ion)
    }

    class ExampleScalarLib(override val ion: IonSystem) : Plugin.ScalarLib {

        @ScalarFunction(
            names = ["to_char_array"],
            description = "Splits $0 into a list of characters",
            returns = "struct"
        )
        fun toCharArray(@Type("string") input: String): IonValue {
            val s = ion.newEmptyStruct()
            val chars = ion.newEmptyList()
            input.forEach { chars.add(ion.newString(it.toString())) }
            s.add("string", ion.newString(input))
            s.add("chars", chars)
            return s
        }

        @ScalarFunction(
            names = ["reverse"],
            description = "Reverse $0",
            returns = "string"
        )
        fun reverse(@Type("string") input: String) = input.reversed()

    }


}

class ExampleSplit(val message: String) : Split

class ExampleSource(val message: String, val times: Long) : SourceHandle


