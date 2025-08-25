package com.niu.demo;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final ThirdPartyService service;

    @GetMapping("/fetch")
    public Object fetch() {
        List<String> data = service.getData();
        return Map.of(
                "size", data.size(),
                "data", data
        );
    }
}
