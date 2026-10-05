package com.mulinocoreano.backend.supplier;

public class SupplierException extends RuntimeException {
    private final SupplierErrorCode code;

    public SupplierException(SupplierErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public SupplierErrorCode code() { return code; }
}
