package org.home.zaval.zavalbackend.persistence

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component

inline fun <reified T> serializeObject(obj: T) = JsonHelper(T::class.java).serializeObject(obj)
inline fun <reified T> deserializeObject(str: String) = JsonHelper(T::class.java).deserializeObject(str)
fun <T> deserializeObject(str: String, clazz: Class<T>) = JsonHelper(clazz).deserializeObject(str)


class JsonHelper<T>(
    private val clazz: Class<T>,
) {
    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)

    fun serializeObject(obj: T): String {
        return mapper.writeValueAsString(obj)
    }

    fun serializeObjectPretty(obj: T): String {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
    }

    fun deserializeObject(str: String): T {
        return mapper.readValue(str, clazz)
    }
}