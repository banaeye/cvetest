package com.example.vulnerable.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // CVE-2021-20190 (CVSS 8.1): enableDefaultTyping は
            // ポリモーフィック型のデシリアライズを許可する。
            // 既知のガジェットクラス(JdbcRowSetImpl 等)と組み合わせると
            // SSRF や任意コード実行につながる。
            // 修正: activateDefaultTyping(validator, JAVA_LANG_OBJECT) かつ
            //       allowlist を使用するか、default typing を無効化する。
            @Suppress("DEPRECATION")
            enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
        }
    }
}
