package com.proje.sosyal

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SosyalApplication

fun main(args: Array<String>) {
    runApplication<SosyalApplication>(*args)
}
