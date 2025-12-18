package ru.practicum.explorewithme;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/illegal-argument")
    public void throwIllegalArgument() {
        throw new IllegalArgumentException("Test illegal argument");
    }

    @GetMapping("/illegal-state")
    public void throwIllegalState() {
        throw new IllegalStateException("Test illegal state");
    }

    @GetMapping("/runtime-exception")
    public void throwRuntimeException() {
        throw new RuntimeException("Test runtime exception");
    }

    @GetMapping("/missing-param")
    public String testMissingParam(String requiredParam) {
        return requiredParam;
    }
}