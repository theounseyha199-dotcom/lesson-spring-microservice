package istad.ha.iamservice.infrastructure.adapter;

import istad.ha.iamservice.application.port.TransactionRunner;
import istad.ha.iamservice.application.exception.ApplicationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Component;
import org.springframework.dao.*;
import java.util.function.Supplier;

@Component
public final class SpringTransactionRunner implements TransactionRunner {
    private final TransactionTemplate read;
    private final TransactionTemplate write;
    public SpringTransactionRunner(PlatformTransactionManager manager) {
        read = new TransactionTemplate(manager); read.setReadOnly(true);
        write = new TransactionTemplate(manager);
    }
    @Override public <T> T read(Supplier<T> operation) { return run(read, operation); }
    @Override public <T> T write(Supplier<T> operation) { return run(write, operation); }
    private <T> T run(TransactionTemplate transaction, Supplier<T> operation) {
        try { return transaction.execute(status -> operation.get()); }
        catch (OptimisticLockingFailureException ex) {
            throw new ApplicationException("concurrent_update", "The record changed; reload and try again");
        } catch (DataIntegrityViolationException ex) {
            throw new ApplicationException("conflict", "A unique identifier or relationship conflicts with existing data");
        }
    }
}
