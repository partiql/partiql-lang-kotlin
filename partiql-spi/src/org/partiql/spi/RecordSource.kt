package org.partiql.spi

import com.amazon.ion.IonValue

interface RecordSource {

  // eh
  fun get(): Sequence<IonValue>

}
