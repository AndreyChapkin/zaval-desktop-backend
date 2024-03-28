package org.home.zaval.zavalbackend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
class DatabaseProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
}