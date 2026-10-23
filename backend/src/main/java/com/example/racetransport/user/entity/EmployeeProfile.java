package com.example.racetransport.user.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thông tin nhân viên (4 role nội bộ).
 * Bảng: employee_profile
 */
@Entity
@Table(name = "employee_profile")
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "department", length = 100)
    private String department;

    /** Chỉ dùng cho DRIVER_ESCORT */
    @Enumerated(EnumType.STRING)
    @Column(name = "staff_role", length = 40)
    private StaffRole staffRole;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "air_escort_certified", nullable = false)
    private Boolean airEscortCertified = Boolean.FALSE;

    @Column(name = "emergency_contact", length = 150)
    private String emergencyContact;

    @Column(name = "base_location", length = 150)
    private String baseLocation;

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

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public StaffRole getStaffRole() { return staffRole; }
    public void setStaffRole(StaffRole staffRole) { this.staffRole = staffRole; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public LocalDate getLicenseExpiry() { return licenseExpiry; }
    public void setLicenseExpiry(LocalDate licenseExpiry) { this.licenseExpiry = licenseExpiry; }

    public Boolean getAirEscortCertified() { return airEscortCertified; }
    public void setAirEscortCertified(Boolean airEscortCertified) { this.airEscortCertified = airEscortCertified; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getBaseLocation() { return baseLocation; }
    public void setBaseLocation(String baseLocation) { this.baseLocation = baseLocation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
