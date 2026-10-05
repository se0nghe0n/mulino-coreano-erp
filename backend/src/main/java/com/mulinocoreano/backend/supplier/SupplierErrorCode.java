package com.mulinocoreano.backend.supplier;

import com.mulinocoreano.backend.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SupplierErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "SUP001", "Supplier not found"),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "SUP002", "Supplier has changed");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SupplierErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
