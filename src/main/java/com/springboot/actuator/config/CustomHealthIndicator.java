package com.springboot.actuator.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Custom health check logic
        boolean isHealthy = false;
        try {
            isHealthy = checkCustomHealth();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (isHealthy) {
            return Health.up().withDetail("CustomHealth", "All systems are operational").build();
        } else {
            return Health.down().withDetail("CustomHealth", "Some systems are down").build();
        }
    }

    private boolean checkCustomHealth() throws InterruptedException {
        // Implement your custom health check logic
        Thread.sleep(10000);
        return true;
    }
}