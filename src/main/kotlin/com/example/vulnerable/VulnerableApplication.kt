package com.example.vulnerable

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VulnerableApplication

fun main(args: Array<String>) {
    runApplication<VulnerableApplication>(*args)
}
