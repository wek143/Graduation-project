package com.graduation.autograding;

import com.graduation.autograding.config.AiAssistantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiAssistantProperties.class)
public class AssignmentAutoGradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssignmentAutoGradingApplication.class, args);
    }
}
