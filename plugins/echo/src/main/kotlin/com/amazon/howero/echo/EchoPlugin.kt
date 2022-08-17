package com.amazon.howero.echo

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

class EchoPlugin(override val ion: IonSystem) : Plugin {

  override fun getSplitSource(source: SourceHandle): SplitSource {
    val s = source as EchoSourceHandle
    return EchoSplitSource(splits = listOf(EchoSplit(s.message, s.times)))
  }

  override fun getRecordSource(split: Split): RecordSource = EchoRecordSource(
      ion = ion,
      split = split as EchoSplit,
  )

  object Sources : SourceResolver() {

    @Source
    fun times(message: String, times: Long): SourceHandle = EchoSourceHandle(message, times)

  }

  class Factory : Plugin.Factory {

    override val identifier: String = "echo"

    override val sourceResolver: SourceResolver = Sources

    override val config: KClass<*> = Any::class

    override fun create(ion: IonSystem, config: Any?): Plugin = EchoPlugin(ion)

  }

}

data class EchoSourceHandle(val message: String, val times: Long) : SourceHandle

data class EchoSplit(val message: String, val times: Long) : Split

class EchoSplitSource(private val splits: List<Split>) : SplitSource {

  private val iterator = splits.iterator();

  override fun hasNext() = iterator.hasNext()

  override fun next(): Split = iterator.next()

  override fun close() {}

}

class EchoRecordSource(
    private val ion: IonSystem,
    private val split: EchoSplit,
) : RecordSource {

  override fun get(): Sequence<IonValue> = sequence {
    (1..split.times).forEach { v ->
      val s = ion.newEmptyStruct()
      s.add("timestamp", ion.newCurrentUtcTimestamp())
      s.add("message", ion.newString(split.message))
      s.add("time", ion.newInt(v))
      yield(s)
    }
  }

}
