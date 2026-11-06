package istad.ha.iamservice.application.command;

/** Distinguishes an omitted PATCH field from an explicitly supplied null. */
public record FieldUpdate<T>(boolean supplied, T value) {
    public static <T> FieldUpdate<T> unchanged() { return new FieldUpdate<>(false, null); }
    public static <T> FieldUpdate<T> set(T value) { return new FieldUpdate<>(true, value); }
    public T orElse(T existing) { return supplied ? value : existing; }
    public static <T> FieldUpdate<T> normalize(FieldUpdate<T> field) { return field == null ? unchanged() : field; }
}
