package org.partiql.plugins

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import org.partiql.spi.*
import kotlin.reflect.KClass

class EchoPlugin(override val ionSystem: IonSystem) : Plugin {

  override fun getSplitSource(source: SourceHandle): SplitSource {
    val s = source as EchoSourceHandle
    return EchoSplitSource(splits = listOf(EchoSplit(s.message, s.times)))
  }

  override fun getRecordSource(split: Split): RecordSource = EchoRecordSource(
      ionSystem = ionSystem,
      split = split as EchoSplit,
  )

  object Sources : SourceResolver() {

    @Source
    fun echo(message: String, times: Long): SourceHandle = EchoSourceHandle(message, times)

  }

  object Factory : Plugin.Factory {

    override val identifier: String = "example"

    override val sourceResolver: SourceResolver = Sources

    override val config: KClass<*> = Any::class

    override fun create(ionSystem: IonSystem, config: Any?): Plugin = EchoPlugin(ionSystem)

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
    private val ionSystem: IonSystem,
    private val split: EchoSplit,
) : RecordSource {

  override fun get(): Sequence<IonValue> = sequence {
    (1..split.times).forEach { v ->
      val s = ionSystem.newEmptyStruct()
      s.add("timestamp", ionSystem.newCurrentUtcTimestamp())
      s.add("message", ionSystem.newString(split.message))
      s.add("time", ionSystem.newInt(v))
      yield(s)
    }
  }

}
