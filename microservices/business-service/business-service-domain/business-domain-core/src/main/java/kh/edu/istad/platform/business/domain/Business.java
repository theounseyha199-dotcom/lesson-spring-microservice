package kh.edu.istad.platform.business.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Business {
    public enum Status { ACTIVE, SUSPENDED }
    private final UUID id;
    private final String name;
    private final String email;
    private final String address;
    private final Status status;
    private final Instant createdAt;

    public Business(UUID id, String name, String email, String address, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Business name is required");
        if (email == null || email.indexOf('@') <= 0 || email.lastIndexOf('.') <= email.indexOf('@') + 1)
            throw new IllegalArgumentException("Valid business email is required");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("Business address is required");
        this.name = name.trim();
        this.email = email.trim();
        this.address = address.trim();
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }
    public UUID id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public String address() { return address; }
    public Status status() { return status; }
    public Instant createdAt() { return createdAt; }
}
