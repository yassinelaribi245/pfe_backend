package tn.star.star_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StarApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarApiApplication.class, args);
    }
}
