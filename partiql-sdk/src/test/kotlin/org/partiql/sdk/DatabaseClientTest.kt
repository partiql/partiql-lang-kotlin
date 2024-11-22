package org.partiql.sdk

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class DatabaseClientTest {

    @Test
    public fun test() {
        val client = DatabaseClient.builder().build()
        val res = runBlocking { client.getInfo {} }
        println(res)
    }
}
