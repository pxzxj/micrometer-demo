package io.github;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MicrometerTest {

    private List<Chore> chores = Arrays.asList(
            new Chore("Mow front lawn", Duration.ofMinutes(20), "yard"),
            new Chore("Mow back lawn", Duration.ofMinutes(10), "yard"),
            new Chore("Gather the laundry", Duration.ofMinutes(7), "laundry"),
            new Chore("Wash the laundry", Duration.ofMinutes(3), "laundry"),
            new Chore("Sort/Fold the laundry", Duration.ofMinutes(50), "laundry"),
            new Chore("Was the dishes", Duration.ofMinutes(10), "kitchen"),
            new Chore("Find my phone charger", Duration.ofMinutes(5))
    );

    @Test
    void testCounterAndTimer() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        for(Chore chore : chores) {
            System.out.println("Doing " + chore.getName());
            meterRegistry.counter("chore.completed").increment();
            meterRegistry.timer("chore.duration").record(chore.getDuration());
        }

        for(Meter meter : meterRegistry.getMeters()) {
            System.out.println(meter.getId() + "   " + meter.measure());
        }
    }

    @Test
    void testCompositeMeterRegistryAndLoggingMeterRegistry() throws InterruptedException {
        CompositeMeterRegistry meterRegistry = Metrics.globalRegistry;
        LoggingRegistryConfig loggingRegistryConfig = new LoggingRegistryConfig() {
            @Override
            public String get(String s) {
                return null;
            }

            @Override
            public boolean logInactive() {
                return true;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(5);
            }
        };
        MeterRegistry loggingRegistry = new LoggingMeterRegistry(loggingRegistryConfig, Clock.SYSTEM);
        meterRegistry.add(loggingRegistry);
        meterRegistry.add(new SimpleMeterRegistry());
        for(Chore chore : chores) {
            System.out.println("Doing " + chore.getName());
            meterRegistry.counter("chore.completed").increment();
            meterRegistry.timer("chore.duration").record(chore.getDuration());
        }
        for(Meter meter : meterRegistry.getMeters()) {
            System.out.println(meter.getId() + "   " + meter.measure());
        }
        for(int i = 1; i < 100; i++) {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("Waiting " + i);
        }
    }

    @Test
    void testTagsAndCommonTags() throws InterruptedException {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.config().commonTags("team", "spring");
        for(Chore chore : chores) {
            System.out.println("Doing " + chore.getName());
            meterRegistry.counter("chore.completed").increment();
            meterRegistry.timer("chore.duration", Tags.of("group", chore.getGroup())).record(chore.getDuration());
        }
        for(Meter meter : meterRegistry.getMeters()) {
            System.out.println(meter.getId() + "   " + meter.measure());
        }
    }

    @Test
    void testGauge() throws InterruptedException {
        CompositeMeterRegistry meterRegistry = Metrics.globalRegistry;
        LoggingRegistryConfig loggingRegistryConfig = new LoggingRegistryConfig() {
            @Override
            public String get(String s) {
                return null;
            }

            @Override
            public boolean logInactive() {
                return true;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(5);
            }
        };
        MeterRegistry loggingRegistry = new LoggingMeterRegistry(loggingRegistryConfig, Clock.SYSTEM);
        meterRegistry.add(loggingRegistry);
        meterRegistry.add(new SimpleMeterRegistry());
        meterRegistry.config().commonTags("team", "spring");
        addGauge(meterRegistry);
        for(Chore chore : chores) {
            System.out.println("Doing " + chore.getName());
            meterRegistry.counter("chore.completed").increment();
            meterRegistry.timer("chore.duration", Tags.of("group", chore.getGroup())).record(chore.getDuration());
        }
        for(Meter meter : meterRegistry.getMeters()) {
            System.out.println(meter.getId() + "   " + meter.measure());
        }
        System.gc();
        for(int i = 1; i < 100; i++) {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("Waiting " + i);
        }
    }

    @Test
    void testMeterFilter() throws InterruptedException {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.config().meterFilter(MeterFilter.deny(id -> id.getName().equals("chore.completed")));
        meterRegistry.config().meterFilter(MeterFilter.maximumAllowableMetrics(2));
        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if(id.getName().equals("chore.duration")) {
                    return id.replaceTags(id.getTags().stream().map(tag -> {
                        if(tag.getKey().equals("group") && tag.getValue().equals("laundry")) {
                            return tag;
                        } else {
                            return Tag.of("group", "other");
                        }
                    }).collect(Collectors.toList()));
                } else {
                    return id;
                }
            }
        });
        meterRegistry.config().commonTags("team", "spring");
        for(Chore chore : chores) {
            System.out.println("Doing " + chore.getName());
            meterRegistry.counter("chore.completed").increment();
            meterRegistry.timer("chore.duration", Tags.of("group", chore.getGroup())).record(chore.getDuration());
        }
        for(Meter meter : meterRegistry.getMeters()) {
            System.out.println(meter.getId() + "   " + meter.measure());
        }
    }

    void addGauge(MeterRegistry meterRegistry) {
        List<Chore> choresList = new ArrayList<>(chores);
        meterRegistry.gauge("chore.size.weak", choresList, List::size);
        meterRegistry.gauge("chore.size.lambda", "", o -> choresList.size());
        Gauge.builder("chore.size.strong", choresList, List::size).strongReference(true).register(meterRegistry);
    }

    static class Chore {

        private String name;
        private Duration duration;
        private String group;

        public Chore(String name, Duration duration, String group) {
            this.name = name;
            this.duration = duration;
            this.group = group;
        }

        public Chore(String name, Duration duration) {
            this.name = name;
            this.duration = duration;
            this.group = "home";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }
    }
}
