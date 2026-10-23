package com.example.racetransport.horse.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Loại chuồng/ngăn vận chuyển khách chọn cho từng ngựa (theo Figma: tiêu chuẩn 75cm, rộng 105cm, hộp 150cm).
 * Bảng: stall_type
 */
@Entity
@Table(name = "stall_type")
public class StallType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "width_cm", nullable = false)
    private Integer widthCm;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "surcharge", nullable = false, precision = 18, scale = 2)
    private BigDecimal surcharge;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getWidthCm() { return widthCm; }
    public void setWidthCm(Integer widthCm) { this.widthCm = widthCm; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getSurcharge() { return surcharge; }
    public void setSurcharge(BigDecimal surcharge) { this.surcharge = surcharge; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
