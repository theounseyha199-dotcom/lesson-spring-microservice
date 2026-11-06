package istad.ha.iamservice;

final class TestDatabaseSettings {
    private TestDatabaseSettings() { }
    static String postgresUrl() {
        String url = System.getenv("IAM_TEST_DB_URL");
        if (url != null && !url.matches("jdbc:postgresql://[^/]+/iam_test_[a-z0-9_]+")) {
            throw new IllegalArgumentException("IAM_TEST_DB_URL must use an isolated database named iam_test_...");
        }
        return url;
    }
    static String username() { return System.getenv().getOrDefault("IAM_TEST_DB_USER", "iteuser"); }
    static String password() { return System.getenv().getOrDefault("IAM_TEST_DB_PASSWORD", ""); }
}
