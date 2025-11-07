package org.veto.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "org.veto.core", "org.veto.admin"})
@PropertySource(value = "file:./application.properties", encoding = "utf-8")
public class AdminService {
    public static void main(String[] args) {
        SpringApplication.run(AdminService.class, args);
    }
}
