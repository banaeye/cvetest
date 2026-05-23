package com.example.vulnerable.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    // CVE-2022-22978 (Spring Security 5.6.2, CVSS 9.8):
    // antMatchers のパターンに正規表現の問題があり、
    // 特殊な URL エンコードで認可チェックをバイパスできる。
    // 修正: Spring Security 5.6.4 / 5.7.1 以上にアップグレード。
    //
    // また H2 コンソール用に frameOptions を無効化しており、
    // Clickjacking のリスクも存在する。
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf().disable()
            .headers().frameOptions().disable()
            .and()
            .authorizeRequests()
            .antMatchers("/h2-console/**").permitAll()
            .antMatchers("/api/**").permitAll()
            .anyRequest().authenticated()
        return http.build()
    }
}
