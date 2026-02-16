package io.flamingock.flags;

import io.flamingock.api.annotations.EnableFlamingock;
import io.flamingock.api.annotations.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFlamingock(
        stages = @Stage(location = "com.flamingock.flags.changes")
)
public class FeatureFlagApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeatureFlagApplication.class, args);
    }
}
