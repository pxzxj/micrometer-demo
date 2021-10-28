package io.github.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HealthToMetricsConverter implements InitializingBean, MeterBinder {

    private Map<String, HealthIndicator> map;

    private ThreadPoolTaskScheduler scheduler;

    private final ConcurrentHashMap<String, Health> latestHealth = new ConcurrentHashMap<>();

    public HealthToMetricsConverter(Map<String, HealthIndicator> map) {
        this.map = map;
        this.scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        scheduler.initialize();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for(Map.Entry<String, HealthIndicator> entry : map.entrySet()) {
            scheduler.scheduleWithFixedDelay(() -> latestHealth.put(entry.getKey(), entry.getValue().health()), Duration.ofSeconds(10));
        }
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for(Map.Entry<String, Health> entry : latestHealth.entrySet()) {
            registry.gauge("health.indicator", Tags.of("name", entry.getKey()), entry.getValue(), health -> {
                Status status = health.getStatus();
                double v = 3.0;
                if(status.equals(Status.UP)) {
                    v = 1.0;
                } else if(status.equals(Status.DOWN)) {
                    v = -1.0;
                } else if(status.equals(Status.OUT_OF_SERVICE)) {
                    v = -2.0;
                }
                return v;
            });
        }
    }
}
