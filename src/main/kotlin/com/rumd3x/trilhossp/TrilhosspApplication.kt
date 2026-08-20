package com.rumd3x.trilhossp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
class TrilhosspApplication

fun main(args: Array<String>) {
    runApplication<TrilhosspApplication>(*args)
}
