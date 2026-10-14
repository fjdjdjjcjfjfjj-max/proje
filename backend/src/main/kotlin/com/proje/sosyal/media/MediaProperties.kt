package com.proje.sosyal.media

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.media")
data class MediaProperties(
    /** Yüklenen görsellerin klasörü. Docker'da kalıcı bir volume'e bağlanır. */
    val dir: String = "./data/media",
    /** Kullanıcı başına saatlik yükleme sınırı (spam ve disk doldurma saldırılarına karşı). */
    val uploadsPerHour: Int = 20,
)
