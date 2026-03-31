package com.moveon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Moveon Bot - AI Personal Assistant
 *
 * Main application entry point.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MoveonBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoveonBotApplication.class, args);
    }
}
