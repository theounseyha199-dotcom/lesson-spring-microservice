package istad.ha.iamservice;

import org.junit.jupiter.api.Test;
import liquibase.Liquibase;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MigrationUpgradeTests {
    @FunctionalInterface interface Check { void run(Connection connection, boolean postgres) throws Exception; }
    void isolated(Check check) throws Exception {
        String url = TestDatabaseSettings.postgresUrl(); boolean postgres = url != null;
        if (!postgres) url = "jdbc:h2:mem:upgrade_" + UUID.randomUUID() + ";MODE=PostgreSQL;INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE";
        try (var connection = DriverManager.getConnection(url, postgres ? TestDatabaseSettings.username() : "sa", postgres ? TestDatabaseSettings.password() : "")) {
            String schema = "iam_upgrade_" + UUID.randomUUID().toString().replace("-", "");
            if (postgres) { sql(connection, "create schema " + schema); connection.setSchema(schema); }
            try { check.run(connection, postgres); }
            finally {
                if (postgres) { if (!connection.getAutoCommit()) connection.rollback(); connection.setAutoCommit(true); connection.setSchema("public"); sql(connection, "drop schema " + schema + " cascade"); }
            }
        }
    }
    static void sql(Connection connection, String statement) throws SQLException { try (var s = connection.createStatement()) { s.execute(statement); } }
    static String scalar(Connection connection, String statement) throws SQLException {
        try (var s = connection.createStatement(); var rows = s.executeQuery(statement)) { assertTrue(rows.next()); return rows.getString(1); }
    }
    static void migrate(Connection connection, String path) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName(connection.getSchema());
        var resource = new ClassLoaderResourceAccessor();
        try { new Liquibase(path, resource, database).update(new Contexts(), new LabelExpression()); }
        finally { resource.close(); }
    }
    static void legacy(Connection connection, boolean postgres) throws Exception {
        if (!postgres) { migrate(connection, "db/legacy-test-changelog.yaml"); return; }
        for (String file : new String[]{"001-create-authrization-table.sql", "002-create-authorization.consent-table.sql", "003-create-client-table.sql", "004-create-core-table.sql", "005-update-user-table.sql"}) {
            migrate(connection, "db/changelog/ddl/" + file);
        }
    }
    static void upgrade(Connection connection, boolean postgres) throws Exception {
        migrate(connection, postgres ? "db/changelog/db.changelog-master.yaml" : "db/test-changelog.yaml");
    }
    static void seedLegacy(Connection connection) throws Exception {
        sql(connection, "insert into users(id, uuid, username, email, given_name, family_name, password, gender, created_by, updated_by) values (700, 'c4e8a2d1-7b3f-4e6a-9d20-5f1b8c3a7e90', 'legacy', 'LEGACY@EXAMPLE.COM', 'Dara', 'Sok', 'preserve-hash', 'Male', 'original-actor', 'original-actor')");
        sql(connection, "insert into roles(id, role) values (500, 'ADMIN')");
        sql(connection, "insert into permissions(id, permission) values (600, 'user:read')");
        sql(connection, "insert into users_roles(user_id, role_id) values (700, 500)");
        sql(connection, "insert into roles_permissions(role_id, permission_id) values (500, 600)");
        sql(connection, "insert into oauth2_registered_client(id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, scopes, client_settings, token_settings) values ('retained-id', 'retained-client', 'retained-client-secret', 'Existing app', 'client_secret_basic', 'authorization_code', 'openid', '{}', '{}')");
    }
    @Test void preservesLegacyUsersCredentialsClientsAndRelationshipsAndIsIdempotent() throws Exception {
        isolated((connection, postgres) -> {
            legacy(connection, postgres); seedLegacy(connection); upgrade(connection, postgres); upgrade(connection, postgres);
            assertEquals("preserve-hash", scalar(connection, "select password_hash from users where id = 700"));
            assertEquals("legacy@example.com", scalar(connection, "select email from users where id = 700"));
            assertEquals("original-actor", scalar(connection, "select created_by from users where id = 700"));
            assertEquals("c4e8a2d1-7b3f-4e6a-9d20-5f1b8c3a7e90", scalar(connection, "select uuid from users where id = 700"));
            assertEquals("retained-client-secret", scalar(connection, "select client_secret from oauth2_registered_client where id = 'retained-id'"));
            assertEquals("1", scalar(connection, "select count(*) from user_roles where user_id = 700 and role_id = 500 and assigned_by = 'migration'"));
            assertEquals("1", scalar(connection, "select count(*) from role_permissions where role_id = 500 and permission_id = 600"));
            assertEquals("1", scalar(connection, "select count(*) from legacy_users_roles where user_id = 700"));
            assertEquals("male", scalar(connection, "select gender from users where id = 700"));
        });
    }
    @Test void refusesOverlengthLegacyFieldsWithoutTruncation() throws Exception {
        isolated((connection, postgres) -> {
            legacy(connection, postgres); seedLegacy(connection);
            String username = "x".repeat(51);
            sql(connection, "update users set username = '" + username + "' where id = 700");
            if (!connection.getAutoCommit()) connection.commit();
            assertThrows(Exception.class, () -> upgrade(connection, postgres));
            if (!connection.getAutoCommit()) connection.rollback();
            assertEquals(username, scalar(connection, "select username from users where id = 700"));
            assertEquals("preserve-hash", scalar(connection, "select password from users where id = 700"));
        });
    }
    @Test void refusesCaseCollidingEmailsBeforeChangingLegacyColumns() throws Exception {
        isolated((connection, postgres) -> {
            legacy(connection, postgres); seedLegacy(connection);
            sql(connection, "insert into users(uuid, username, email, given_name, family_name) values ('7f3d2a10-91c4-4b0e-8d6f-2a9c5e1b7d40', 'collision', 'legacy@example.com', 'Other', 'User')");
            assertThrows(Exception.class, () -> upgrade(connection, postgres));
            if (!connection.getAutoCommit()) connection.rollback();
            assertEquals("preserve-hash", scalar(connection, "select password from users where id = 700"));
            assertEquals("2", scalar(connection, "select count(*) from users"));
        });
    }
}
