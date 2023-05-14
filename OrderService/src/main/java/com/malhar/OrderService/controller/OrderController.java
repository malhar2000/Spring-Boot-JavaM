package com.malhar.OrderService.controller;

import com.malhar.OrderService.dto.OrderRequest;
import com.malhar.OrderService.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/order")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "inventory", fallbackMethod = "fallbackMethodTimeOut")
    public CompletableFuture<String> placeOrder(@RequestBody OrderRequest orderRequest){
         return CompletableFuture.supplyAsync(() -> orderService.placeOrder(orderRequest));

    }

    // need to have same return type as the original method
    public CompletableFuture<String> fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException){
        return CompletableFuture.supplyAsync(() -> "Something went wrong!!");
    }

    public CompletableFuture<String> fallbackMethodTimeOut(OrderRequest orderRequest, RuntimeException runtimeException){
        return CompletableFuture.supplyAsync(() -> "TimeOut!!");
    }
}
