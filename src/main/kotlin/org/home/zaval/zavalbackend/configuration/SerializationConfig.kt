package org.home.zaval.zavalbackend.configuration

import org.home.zaval.zavalbackend.entity.value.TodoStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.function.Consumer

@Configuration
class SerializationConfig: WebMvcConfigurer {

    @Bean
    fun todoStatusConverter(): Converter<String, TodoStatus> = TodoStatusConverter()
}

class TodoStatusConverter: Converter<String, TodoStatus> {
    override fun convert(source: String): TodoStatus {
        val fixedSource = source
            .replace("-", "_")
            .uppercase()
        return TodoStatus.valueOf(fixedSource)
    }
}