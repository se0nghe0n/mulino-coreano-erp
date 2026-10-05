package com.mulinocoreano.backend.supplier;

import com.mulinocoreano.backend.common.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "suppliers")
public class Supplier extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long id;
    @Column(length = 100, nullable = false)
    private String name;
    @Column(length = 50, nullable = false)
    private String country;
    @Column(length = 50)
    private String contactName;
    @Column(length = 100)
    private String contactEmail;
    @Column(length = 20)
    private String contactPhone;
    @Column(length = 200)
    private String addressLine;
    @Column(length = 100)
    private String city;
    @Column(length = 20)
    private String postalCode;
    @Column(length = 3, nullable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_terms")
    private PaymentTerms paymentTerms;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Version
    @Column(nullable = false)
    private long version;

    protected Supplier() {}

    public Supplier(CreateSupplierRequest input) {
        replace(input);
    }

    public void replace(CreateSupplierRequest input) {
        name = input.name();
        country = input.country();
        contactName = input.contactName();
        contactEmail = input.contactEmail();
        contactPhone = input.contactPhone();
        addressLine = input.addressLine();
        city = input.city();
        postalCode = input.postalCode();
        currency = input.currency() == null ? "KRW" : input.currency();
        paymentTerms = input.paymentTerms() == null ? PaymentTerms.NET_30 : input.paymentTerms();
    }

    public void deactivate() {
        active = false;
    }

    public long version() {
        return version;
    }

    public SupplierResponse response() {
        return new SupplierResponse(id, name, country, contactName, contactEmail, contactPhone,
                addressLine, city, postalCode, paymentTerms, currency, active, version, createdAt, updatedAt);
    }
}
