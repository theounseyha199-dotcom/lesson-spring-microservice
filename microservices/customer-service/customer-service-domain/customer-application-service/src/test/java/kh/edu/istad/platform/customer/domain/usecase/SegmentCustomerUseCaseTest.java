package kh.edu.istad.platform.customer.domain.usecase;
import kh.edu.istad.platform.customer.domain.entity.CustomerSegment;
import kh.edu.istad.platform.customer.domain.exception.*;
import kh.edu.istad.platform.customer.domain.port.out.SegmentRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SegmentCustomerUseCaseTest {
    private final SegmentRepository repository = mock(SegmentRepository.class);
    private final SegmentCustomerUseCase service = new SegmentCustomerUseCase(repository);
    private final UUID id = UUID.randomUUID();
    private CustomerSegment segment(CustomerSegment.Status status) {
        return new CustomerSegment(id, "VIP", "High value", status, Instant.now(), Instant.now(), 0L);
    }
    @Test void missingSegmentIsNotFound() {
        when(repository.findById(id)).thenReturn(Mono.empty());
        StepVerifier.create(service.get(id)).expectError(ResourceNotFoundException.class).verify();
    }
    @Test void inactiveSegmentsRejectNewMemberships() {
        when(repository.findById(id)).thenReturn(Mono.just(segment(CustomerSegment.Status.INACTIVE)));
        StepVerifier.create(service.requireAssignable(id)).expectError(ConflictException.class).verify();
    }
    @Test void activeSegmentAndUnassignedMembershipAreAccepted() {
        when(repository.findById(id)).thenReturn(Mono.just(segment(CustomerSegment.Status.ACTIVE)));
        StepVerifier.create(service.requireAssignable(id)).verifyComplete();
        StepVerifier.create(service.requireAssignable(null)).verifyComplete();
    }
    @Test void deletionPreservesSegmentIdentityAndVersion() {
        when(repository.findById(id)).thenReturn(Mono.just(segment(CustomerSegment.Status.ACTIVE)));
        when(repository.save(any())).thenAnswer(call -> Mono.just(call.getArgument(0)));
        StepVerifier.create(service.delete(id)).verifyComplete();
        var saved = org.mockito.ArgumentCaptor.forClass(CustomerSegment.class);
        verify(repository).save(saved.capture());
        assertEquals(id, saved.getValue().segmentId());
        assertEquals(CustomerSegment.Status.DELETED, saved.getValue().status());
        assertEquals(0L, saved.getValue().version());
    }
    @Test void deletedStatusCannotBypassTheDeleteOperation() {
        when(repository.findById(id)).thenReturn(Mono.just(segment(CustomerSegment.Status.ACTIVE)));
        StepVerifier.create(service.update(id, "VIP", "", CustomerSegment.Status.DELETED))
                .expectError(IllegalArgumentException.class).verify();
        verify(repository, never()).save(any());
    }
    @Test void rejectsInvalidSegmentNamesBeforePersistence() {
        StepVerifier.create(service.create(" ", "")).expectError(IllegalArgumentException.class).verify();
        verifyNoInteractions(repository);
    }
}
