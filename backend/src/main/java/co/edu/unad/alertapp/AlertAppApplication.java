package co.edu.unad.alertapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlertAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertAppApplication.class, args);
    }
}
