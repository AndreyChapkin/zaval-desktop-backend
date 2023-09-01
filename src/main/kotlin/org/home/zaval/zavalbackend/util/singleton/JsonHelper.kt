package org.home.zaval.zavalbackend.util.singleton

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JsonHelper {
    val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)

    fun serializeObject(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }

    fun serializeObjectPretty(obj: Any): String {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
    }

    inline fun <reified T> deserializeObject(str: String): T {
        return mapper.readValue(str, T::class.java)
    }
}