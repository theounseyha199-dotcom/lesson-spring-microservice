package istad.ha.iamservice.application.port;

public interface TransactionRunner {
    <T> T read(java.util.function.Supplier<T> operation);
    <T> T write(java.util.function.Supplier<T> operation);
    default void write(Runnable operation) { write(() -> { operation.run(); return null; }); }
}
