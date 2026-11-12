package com.example.racetransport.horse.entity;

import com.example.racetransport.user.entity.AppUser;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hồ sơ ngựa. Lưu trữ (ARCHIVED) thay vì xóa cứng khi đã có lịch sử.
 * Bảng: horse
 */
@Entity
@Table(name = "horse")
public class Horse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Khách hàng sở hữu */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "registered_name", nullable = false, length = 150)
    private String registeredName;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", length = 40)
    private HorseSex sex;

    @Column(name = "color", length = 60)
    private String color;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "microchip_number", nullable = false, unique = true, length = 30)
    private String microchipNumber;

    @Column(name = "passport_number", nullable = false, unique = true, length = 50)
    private String passportNumber;

    @Column(name = "passport_country", length = 2)
    private String passportCountry;

    @Column(name = "special_requirements", length = 1000)
    private String specialRequirements;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private HorseStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }

    public String getRegisteredName() { return registeredName; }
    public void setRegisteredName(String registeredName) { this.registeredName = registeredName; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public HorseSex getSex() { return sex; }
    public void setSex(HorseSex sex) { this.sex = sex; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public String getMicrochipNumber() { return microchipNumber; }
    public void setMicrochipNumber(String microchipNumber) { this.microchipNumber = microchipNumber; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getPassportCountry() { return passportCountry; }
    public void setPassportCountry(String passportCountry) { this.passportCountry = passportCountry; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public HorseStatus getStatus() { return status; }
    public void setStatus(HorseStatus status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
