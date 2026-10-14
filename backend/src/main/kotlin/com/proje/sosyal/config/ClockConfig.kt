package com.proje.sosyal.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** Zamana bağlı mantığın (yaş grubu geçişi, token süresi vb.) test edilebilmesi için tek saat kaynağı. */
@Configuration(proxyBeanMethods = false)
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
