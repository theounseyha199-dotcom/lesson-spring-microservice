package kh.edu.istad.platform.order.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class QueryCustomerResult {
    private UUID customerId;
    private String username;
    private String familyName;
    private String givenName;
    private String email;
    private String phoneNumber;
    private String customerStatus;
    private String customerSegmentType;
}
