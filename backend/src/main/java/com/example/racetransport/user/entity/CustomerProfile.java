package com.example.racetransport.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thông tin riêng của khách hàng (1:1 với app_user role CUSTOMER).
 * Bảng: customer_profile
 */
@Entity
@Table(name = "customer_profile")
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 40)
    private CustomerType customerType;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    @Column(name = "preferred_language", nullable = false, length = 5)
    private String preferredLanguage = "vi";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
