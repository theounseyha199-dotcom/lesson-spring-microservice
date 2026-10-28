package kh.edu.istad.platform.order.discovery;

import kh.edu.istad.platform.order.client.adapter.CustomerServiceClientAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerDiscoveryControllerTest {
    @Test
    void customerCollectionIsAvailableAtBrowserAndApiPaths() throws Exception {
        var client = mock(CustomerServiceClientAdapter.class);
        var id = UUID.randomUUID();
        when(client.findAll()).thenReturn(Mono.just(List.of(
                new CustomerServiceClientAdapter.CustomerSummary(id, "test", "Chan", "Sokha2",
                        "sokha.chan@example.com", "+85512345678", "ACTIVE", null))));
        var mvc = MockMvcBuilders.standaloneSetup(new CustomerDiscoveryController(client)).build();
        for (String path : List.of("/customers", "/api/discovery/customers")) {
            var pending = mvc.perform(get(path)).andExpect(request().asyncStarted()).andReturn();
            mvc.perform(asyncDispatch(pending)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].customerId").value(id.toString()))
                    .andExpect(jsonPath("$[0].username").value("test"))
                    .andExpect(jsonPath("$[0].familyName").value("Chan"))
                    .andExpect(jsonPath("$[0].givenName").value("Sokha2"))
                    .andExpect(jsonPath("$[0].email").value("sokha.chan@example.com"))
                    .andExpect(jsonPath("$[0].phoneNumber").value("+85512345678"))
                    .andExpect(jsonPath("$[0].customerStatus").value("ACTIVE"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("\"customerSegmentType\":null")));
        }
    }

    @Test
    void unavailableCustomerServiceReturns503() throws Exception {
        var client = mock(CustomerServiceClientAdapter.class);
        when(client.findAll()).thenReturn(Mono.error(new IllegalStateException("offline")));
        var mvc = MockMvcBuilders.standaloneSetup(new CustomerDiscoveryController(client)).build();
        var pending = mvc.perform(get("/customers")).andReturn();
        mvc.perform(asyncDispatch(pending)).andExpect(status().isServiceUnavailable());
    }
}
