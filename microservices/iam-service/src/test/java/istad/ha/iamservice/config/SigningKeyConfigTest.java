package istad.ha.iamservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import static org.junit.jupiter.api.Assertions.*;

class SigningKeyConfigTest {
    @TempDir Path directory;
    @Test
    void restartReusesPrivateKeyWithRestrictedPermissions() throws Exception {
        Path file = directory.resolve("key.json");
        var first = SigningKeyConfig.loadOrCreate(file);
        var second = SigningKeyConfig.loadOrCreate(file);
        assertEquals(first.toJSONString(), second.toJSONString());
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
    }
    @Test
    void corruptExistingKeyFailsWithoutReplacingIt() throws Exception {
        Path file = directory.resolve("key.json");
        Files.writeString(file, "corrupt");
        assertThrows(Exception.class, () -> SigningKeyConfig.loadOrCreate(file));
        assertEquals("corrupt", Files.readString(file));
    }
}
