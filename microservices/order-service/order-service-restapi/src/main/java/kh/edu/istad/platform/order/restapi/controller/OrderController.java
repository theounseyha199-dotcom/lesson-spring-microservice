package kh.edu.istad.platform.order.restapi.controller;

import jakarta.validation.Valid;
import kh.edu.istad.platform.order.domain.usecase.CreateOrderUseCase;
import kh.edu.istad.platform.order.restapi.dto.CreateOrderRequest;
import kh.edu.istad.platform.order.restapi.dto.OrderResponse;
import kh.edu.istad.platform.order.restapi.mapper.OrderWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderUseCase createOrder;
    private final OrderWebMapper mapper;

    public OrderController(CreateOrderUseCase createOrder, OrderWebMapper mapper) {
        this.createOrder = createOrder;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return mapper.toResponse(createOrder.execute(mapper.toCommand(request)));
    }
}
