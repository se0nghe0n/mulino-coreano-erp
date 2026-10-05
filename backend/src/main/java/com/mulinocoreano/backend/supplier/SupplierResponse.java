package com.mulinocoreano.backend.supplier;
import java.time.LocalDateTime;
public record SupplierResponse(long supplierId,String name,String country,String contactName,String contactEmail,String contactPhone,
    String addressLine,String city,String postalCode,PaymentTerms paymentTerms,String currency,boolean active,long version,
    LocalDateTime createdAt,LocalDateTime updatedAt) {}
