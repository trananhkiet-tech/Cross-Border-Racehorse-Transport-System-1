package com.example.racetransport.transportationrequest.entity;

import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.StallType;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Quan hệ N:N yêu cầu – ngựa, kèm loại chuồng và giá trị khai báo.
 * Bảng: request_horse
 */
@Entity
@Table(name = "request_horse", uniqueConstraints = {
        @UniqueConstraint(name = "uq_request_horse_request_id_horse_id", columnNames = {"request_id", "horse_id"})})
public class RequestHorse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private TransportationRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horse_id", nullable = false)
    private Horse horse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stall_type_id", nullable = false)
    private StallType stallType;

    /** Giá trị khai báo – chỉ tham chiếu bảo hiểm/hải quan/claim */
    @Column(name = "declared_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "notes", length = 500)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TransportationRequest getRequest() { return request; }
    public void setRequest(TransportationRequest request) { this.request = request; }

    public Horse getHorse() { return horse; }
    public void setHorse(Horse horse) { this.horse = horse; }

    public StallType getStallType() { return stallType; }
    public void setStallType(StallType stallType) { this.stallType = stallType; }

    public BigDecimal getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(BigDecimal declaredValue) { this.declaredValue = declaredValue; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
