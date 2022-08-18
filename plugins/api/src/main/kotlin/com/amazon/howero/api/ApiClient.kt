package com.amazon.howero.api

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ApiClient {


    // https://api.covid19api.com/summary
    // https://api.dictionaryapi.dev/api/v2/entries/en/guitar
    // a little unbounded cache action
    private val cache: Cache<String, IonValue> = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build()

    fun load(uri: String, ion: IonSystem): IonValue = cache.get(uri) {
        val url = URL(uri)
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connect()
        if (conn.responseCode != 200) {
            throw RuntimeException("HttpResponseCode: ${conn.responseCode}");
        } else {
            val loader = ion.newLoader()
            val gram = loader.load(url.openStream().readAllBytes())
            gram[0]
        }
    }

}
