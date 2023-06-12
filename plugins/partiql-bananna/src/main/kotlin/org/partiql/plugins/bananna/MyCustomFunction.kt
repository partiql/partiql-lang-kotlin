package org.partiql.plugins.bananna

import org.partiql.spi.functions.CustomInterface

public class MyCustomFunction : CustomInterface {
    override fun performAction() {
        println("Action performed by MyCustomFunction")
    }
}
