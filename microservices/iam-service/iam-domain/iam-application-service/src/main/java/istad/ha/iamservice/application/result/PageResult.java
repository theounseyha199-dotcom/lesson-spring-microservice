package istad.ha.iamservice.application.result;

public record PageResult<T>(java.util.List<T> content, PageMetadata page) {
    public PageResult { content = java.util.List.copyOf(content); java.util.Objects.requireNonNull(page); }
    public record PageMetadata(int number, int size, long totalElements, long totalPages) { }
    public static <T> PageResult<T> of(java.util.List<T> content, int number, int size, long total) {
        if (number < 0 || size < 1 || total < 0) throw new IllegalArgumentException("Invalid page metadata");
        return new PageResult<>(content, new PageMetadata(number, size, total, total / size + (total % size == 0 ? 0 : 1)));
    }
    public <R> PageResult<R> map(java.util.function.Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page);
    }
}
