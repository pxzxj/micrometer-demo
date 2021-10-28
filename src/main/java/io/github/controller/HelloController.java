package io.github.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@RestController
public class HelloController {

    private Counter counter;

    private RestTemplate restTemplate;

    public HelloController(MeterRegistry meterRegistry, RestTemplateBuilder builder) {
        this.counter = meterRegistry.counter("demo.http.requests.total", Tags.of("uri", "/hello"));
        this.restTemplate = builder.build();
    }

    @GetMapping("/hello")
    public String hello() {
        counter.increment();
        return "Hello Micrometer!";
    }

    @GetMapping("/restwithuritemplate")
    public Map<String, String> restWithUriTemplate(String suffix) {
        return Collections.singletonMap("html", restTemplate.getForObject("https://tieba.baidu.com/{suffix}", String.class, suffix));
    }

    @GetMapping("/restwithouturitemplate")
    public Map<String, String> restWithoutUriTemplate(String suffix) {
        return Collections.singletonMap("html", restTemplate.getForObject("https://tieba.baidu.com/" + suffix, String.class));
    }

}
