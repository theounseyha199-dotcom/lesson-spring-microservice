package kh.edu.istad.platform.customer.persistence;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import kh.edu.istad.platform.customer.domain.entity.Customer;
import kh.edu.istad.platform.customer.domain.port.out.CustomerRepository;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import kh.edu.istad.platform.customer.domain.valueobject.Email;
import kh.edu.istad.platform.customer.domain.valueobject.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostgresCustomerRepository implements CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Customer save(Customer customer) {
        jdbcTemplate.update("""
                INSERT INTO customers (customer_id, username, family_name, given_name, email, phone_number, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (customer_id) DO UPDATE SET
                    username = EXCLUDED.username,
                    family_name = EXCLUDED.family_name,
                    given_name = EXCLUDED.given_name,
                    email = EXCLUDED.email,
                    phone_number = EXCLUDED.phone_number,
                    status = EXCLUDED.status
                """,
                customer.getId().value(),
                customer.getUsername(),
                customer.getFamilyName(),
                customer.getGivenName(),
                customer.getEmail().value(),
                customer.getPhoneNumber().value(),
                customer.getStatus().name()
        );
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jdbcTemplate.query("""
                SELECT customer_id, username, family_name, given_name, email, phone_number, status
                FROM customers WHERE customer_id = ?
                """, (rs, rowNum) -> Customer.builder()
                        .id(new CustomerId(rs.getObject("customer_id", UUID.class)))
                        .username(rs.getString("username"))
                        .familyName(rs.getString("family_name"))
                        .givenName(rs.getString("given_name"))
                        .email(new Email(rs.getString("email")))
                        .phoneNumber(new PhoneNumber(rs.getString("phone_number")))
                        .status(CustomerStatus.valueOf(rs.getString("status")))
                        .build(), id.value()).stream().findFirst();
    }
}
