package com.amazon.howero.fs

import com.amazon.ion.IonSystem
import org.partiql.spi.Plugin
import org.partiql.spi.ScalarFunction
import org.partiql.spi.Type

class FSScalarLib(override val ion: IonSystem) : Plugin.ScalarLib {

    @ScalarFunction(
        names = ["howero"],
        description = "Appends 'howero' to $0",
        returns = "string",
    )
    fun howero(@Type("string") str: String): String = str + "howero"

}
