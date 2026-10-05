package com.mulinocoreano.backend.supplier;
import jakarta.validation.constraints.*;
public record CreateSupplierRequest(
    @NotBlank @Size(max=100) String name,
    @NotBlank @Size(max=50) String country,
    @Size(max=50) String contactName,
    @Email @Size(max=100) String contactEmail,
    @Size(max=20) String contactPhone,
    @Size(max=200) String addressLine,
    @Size(max=100) String city,
    @Size(max=20) String postalCode,
    PaymentTerms paymentTerms,
    @Pattern(regexp="[A-Z]{3}") String currency
) {}
