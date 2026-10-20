package kh.edu.istad.platform.customer.restapi.controller;

import kh.edu.istad.platform.customer.domain.dto.CustomerStatistics;
import kh.edu.istad.platform.customer.domain.dto.FilterRequest;
import kh.edu.istad.platform.customer.domain.usecase.QueryCustomerUseCase;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Root statistics alias; both routes use the same filtering and aggregation implementation. */
@RestController
public class StatisticsController {
    private final QueryCustomerUseCase customers;

    public StatisticsController(QueryCustomerUseCase customers) { this.customers = customers; }

    @GetMapping("/statistics")
    public Mono<CustomerStatistics> statistics(@RequestParam MultiValueMap<String, String> parameters,
            @RequestParam(name="search", required=false) String search) {
        return customers.statistics(new FilterRequest(parameters.get("filter"), search, 0, 20, "createdAt", "desc"));
    }
}
