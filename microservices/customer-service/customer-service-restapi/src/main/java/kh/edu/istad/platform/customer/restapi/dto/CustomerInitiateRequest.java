package kh.edu.istad.platform.customer.restapi.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerInitiateRequest(
        @NotBlank
        String username,
        @NotBlank
        String familyName,
        @NotBlank
        String givenName,
        @NotBlank
        @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.Size(max = 254)
        String email,
        @NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "\\+?[0-9]{8,15}")
        String phoneNumber,
        java.util.UUID segmentId
) {
}
