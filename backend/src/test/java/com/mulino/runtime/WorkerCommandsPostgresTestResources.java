package com.mulino.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Class-scoped resources; only the default path owns and stops its container. */
final class WorkerCommandsPostgresTestResources implements AutoCloseable {
    private static final String IMAGE =
            "postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280";

    final KeyPair keyPair;
    final Path publicKeyPath;
    final Path blobRoot;
    private final PostgreSQLContainer container;
    private final WorkerCommandsPostgresTarget externalDatabase;
    private boolean closed;

    private WorkerCommandsPostgresTestResources(KeyPair keyPair, Path publicKeyPath,
                                                 Path blobRoot, PostgreSQLContainer container,
                                                 WorkerCommandsPostgresTarget externalDatabase) {
        this.keyPair = keyPair;
        this.publicKeyPath = publicKeyPath;
        this.blobRoot = blobRoot;
        this.container = container;
        this.externalDatabase = externalDatabase;
    }

    static WorkerCommandsPostgresTestResources open(Map<String, String> environment) {
        WorkerCommandsPostgresTarget external =
                WorkerCommandsPostgresTarget.fromEnvironment(environment).orElse(null);
        PostgreSQLContainer container = null;
        boolean started = false;
        Path publicKey = null;
        Path blobs = null;
        try {
            Path tempDirectory = Path.of(System.getProperty("java.io.tmpdir"));
            if (!Files.isDirectory(tempDirectory)) {
                throw new IllegalStateException("Configured Java temporary directory is unavailable");
            }
            if (external == null) {
                container = new PostgreSQLContainer(IMAGE);
                started = true;
                container.start();
            } else {
                external.verifyFreshDatabase();
            }

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            blobs = Files.createTempDirectory(tempDirectory, "runtime-intake-blobs-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            publicKey = Files.createTempFile(tempDirectory, "runtime-intake-public-", ".pem",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.writeString(publicKey, "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(keyPair.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n");
            return new WorkerCommandsPostgresTestResources(
                    keyPair, publicKey, blobs, container, external);
        } catch (Exception failure) {
            cleanupAfterFailedStartup(container, started, publicKey, blobs, failure);
            throw new IllegalStateException("Cannot initialize WorkerCommandsPostgresTest resources", failure);
        }
    }

    void properties(DynamicPropertyRegistry registry) {
        if (externalDatabase == null) {
            registry.add("spring.datasource.url", container::getJdbcUrl);
            registry.add("spring.datasource.username", container::getUsername);
            registry.add("spring.datasource.password", container::getPassword);
        } else {
            registry.add("spring.datasource.url", externalDatabase::jdbcUrl);
            registry.add("spring.datasource.username", externalDatabase::username);
            registry.add("spring.datasource.password", externalDatabase::password);
        }
        registry.add("JWT_PUBLIC_KEY", publicKeyPath::toString);
        registry.add("JWT_ISSUER", () -> "https://runtime-fixture.invalid");
        registry.add("JWT_AUDIENCE", () -> "mulino-test");
        registry.add("mulino.verification.instant", () -> "2026-10-07T09:00:00Z");
        registry.add("mulino.evidence.blob-root", blobRoot::toString);
        registry.add("server.address", () -> "127.0.0.1");
    }

    @Override public void close() throws Exception {
        if (closed) return;
        closed = true;
        Exception failure = null;
        if (container != null) {
            try {
                container.stop();
            } catch (Exception stopFailure) {
                failure = stopFailure;
            }
        }
        failure = attempt(failure, () -> Files.deleteIfExists(publicKeyPath));
        failure = attempt(failure, () -> deleteTree(blobRoot));
        if (failure != null) throw failure;
    }

    private static void cleanupAfterFailedStartup(PostgreSQLContainer container, boolean started,
                                                  Path publicKey, Path blobRoot, Exception failure) {
        if (started && container != null) {
            try {
                container.stop();
            } catch (Exception cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
        try {
            if (publicKey != null) Files.deleteIfExists(publicKey);
        } catch (Exception cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
        try {
            if (blobRoot != null) deleteTree(blobRoot);
        } catch (Exception cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private static Exception attempt(Exception prior, ThrowingAction action) {
        try {
            action.run();
        } catch (Exception cleanupFailure) {
            if (prior == null) return cleanupFailure;
            prior.addSuppressed(cleanupFailure);
        }
        return prior;
    }

    private static void deleteTree(Path root) throws java.io.IOException {
        if (Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
}
