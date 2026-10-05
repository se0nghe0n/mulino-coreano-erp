package com.mulinocoreano.backend.supplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record UpdateSupplierRequest(@NotNull @Valid CreateSupplierRequest supplier, @NotNull @PositiveOrZero Long expectedVersion) {}
