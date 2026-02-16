package com.example.flags;

import io.flamingock.api.annotations.EnableFlamingock;
import io.flamingock.api.annotations.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFlamingock(
        stages = @Stage(name = "flags", location = "com.example.flags.changes")
)
public class FeatureFlagApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeatureFlagApplication.class, args);
    }
}
