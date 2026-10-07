package org.mulino.verification;

import java.nio.file.Path;
import org.mulino.verification.actual.ActualAcceptanceDriver;

/** Actual adapters require explicit selection; RED always retains the original missing driver. */
public final class DriverFactory {
    public static AcceptanceDriver create(Path root, boolean red) {
        if (red) return new UnimplementedDriver();
        return switch (System.getProperty("verification.driver", "unimplemented")) {
            case "unimplemented" -> new UnimplementedDriver();
            case "actual" -> ActualAcceptanceDriver.fromEnvironment(root);
            default -> throw new IllegalArgumentException("Unknown verification.driver");
        };
    }
    private DriverFactory() {}
}
