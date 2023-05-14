package com.malhar.OrderService.service;

import com.malhar.OrderService.dto.InventoryResponse;
import com.malhar.OrderService.dto.OrderLineItemsDto;
import com.malhar.OrderService.dto.OrderRequest;
import com.malhar.OrderService.event.OrderPlacedEvent;
import com.malhar.OrderService.model.Order;
import com.malhar.OrderService.model.OrderLineItems;
import com.malhar.OrderService.repository.OrderRepo;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepo orderRepo;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsList().stream().map(this::mapToDto).toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(orderLineItem -> orderLineItem.getSkuCode()).toList();

        // call inventory service, and place order if product in stock
        // using webclient by default makes an async request but we need sync req.. (therefore the block())
        InventoryResponse[] res = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                        .retrieve()
                                .bodyToMono(InventoryResponse[].class)
                                        .block();

        boolean allInStock = Arrays.stream(res).allMatch(inventoryResponse -> inventoryResponse.getIsInStock());
        if(allInStock){
            orderRepo.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
            return "Order Placed!";
        } else{
            throw new IllegalArgumentException("Product not in stock please try again!");
        }

    }
    // from orderLinesItemsDto to OrderLineItems
    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
