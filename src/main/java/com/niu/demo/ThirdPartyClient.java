package com.niu.demo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class ThirdPartyClient {

    // 模拟：有概率返回空列表
    public Optional<List<String>> fetchData() {
        boolean empty = ThreadLocalRandom.current().nextInt(100) < 35; // 35%概率无数据
        if (empty) return Optional.of(List.of());          // “无数据”=空集合
        return Optional.of(List.of("a","b","c"));          // 有数据
    }
}
