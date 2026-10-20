package kh.edu.istad.platform.customer.restapi.controller;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.usecase.CreateCustomerUseCase;
import kh.edu.istad.platform.customer.domain.usecase.DeleteCustomerUseCase;
import kh.edu.istad.platform.customer.domain.usecase.DeactivateCustomerUseCase;
import kh.edu.istad.platform.customer.domain.usecase.QueryCustomerUseCase;
import kh.edu.istad.platform.customer.domain.usecase.UpdateCustomerUseCase;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import kh.edu.istad.platform.customer.restapi.mapper.CustomerWebMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerQueryEndpointTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CustomerController(
            mock(CreateCustomerUseCase.class), new QueryCustomerUseCase(repository),
            mock(UpdateCustomerUseCase.class), mock(DeactivateCustomerUseCase.class),
            mock(DeleteCustomerUseCase.class), new CustomerWebMapperImpl())).setControllerAdvice(new CustomerErrorHandler()).build();

    @Test
    void returnsCustomersWithFlatFieldsAndStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(new CustomerId(id))
                .username("sokha")
                .familyName("Chan")
                .givenName("Sokha")
                .email(new Email("sokha@example.com"))
                .phoneNumber(new PhoneNumber("+85512345678"))
                .status(CustomerStatus.INACTIVE)
                .build();
        when(repository.findAll(any(kh.edu.istad.platform.customer.domain.dto.FilterRequest.class))).thenReturn(Flux.just(customer));

        var pending = mvc.perform(get("/api/customers"))
                .andExpect(request().asyncStarted()).andReturn();

        mvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerId").value(id.toString()))
                .andExpect(jsonPath("$[0].username").value("sokha"))
                .andExpect(jsonPath("$[0].familyName").value("Chan"))
                .andExpect(jsonPath("$[0].givenName").value("Sokha"))
                .andExpect(jsonPath("$[0].email").value("sokha@example.com"))
                .andExpect(jsonPath("$[0].phoneNumber").value("+85512345678"))
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));
        verify(repository).findAll(any(kh.edu.istad.platform.customer.domain.dto.FilterRequest.class));
    }

    @Test
    void returnsEmptyArrayWhenThereAreNoCustomers() throws Exception {
        when(repository.findAll(any(kh.edu.istad.platform.customer.domain.dto.FilterRequest.class))).thenReturn(Flux.empty());

        var pending = mvc.perform(get("/api/customers"))
                .andExpect(request().asyncStarted()).andReturn();

        mvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
