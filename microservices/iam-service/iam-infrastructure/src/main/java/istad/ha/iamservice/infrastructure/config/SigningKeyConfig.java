package istad.ha.iamservice.infrastructure.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SigningKeyConfig {
    @Bean
    JWKSource<SecurityContext> jwkSource(@Value("${app.iam.signing-key-file}") String file) throws Exception {
        return new ImmutableJWKSet<>(new JWKSet(loadOrCreate(Path.of(file))));
    }

    static synchronized RSAKey loadOrCreate(Path file) throws Exception {
        file = file.toAbsolutePath();
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            var key = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate()).keyID(UUID.randomUUID().toString()).build();
            try {
                Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                Files.writeString(file, key.toJSONString());
            } catch (FileAlreadyExistsException ignored) { }
        }
        var key = RSAKey.parse(Files.readString(file));
        if (!key.isPrivate() || key.getKeyID() == null) throw new IllegalStateException("Signing key must contain a private RSA key and key ID");
        return key;
    }
}
