package kh.edu.istad.platform.customer.restapi.controller;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.usecase.*;
import kh.edu.istad.platform.customer.restapi.mapper.CustomerWebMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerDeleteEndpointTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CustomerController(
            mock(CreateCustomerUseCase.class), mock(QueryCustomerUseCase.class),
            mock(UpdateCustomerUseCase.class), mock(DeactivateCustomerUseCase.class),
            new DeleteCustomerUseCase(repository), new CustomerWebMapperImpl())).setControllerAdvice(new CustomerErrorHandler()).build();

    @Test
    void returnsNoContentAfterDeletingCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.deleteById(new CustomerId(id))).thenReturn(reactor.core.publisher.Mono.just(true));

        var pending = mvc.perform(delete("/api/customers/{customerId}", id)).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(pending))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(repository).deleteById(new CustomerId(id));
    }

    @Test
    void returnsNotFoundWhenCustomerDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.deleteById(new CustomerId(id))).thenReturn(reactor.core.publisher.Mono.just(false));

        var pending = mvc.perform(delete("/api/customers/{customerId}", id)).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(pending))
                .andExpect(status().isNotFound());
        verify(repository).deleteById(new CustomerId(id));
    }

    @Test
    void rejectsInvalidCustomerIdBeforeDeletingAnything() throws Exception {
        mvc.perform(delete("/api/customers/not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }
}
