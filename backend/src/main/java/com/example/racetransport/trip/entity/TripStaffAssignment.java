package com.example.racetransport.trip.entity;

import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.StaffRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Phân công tài xế/áp tải/trưởng nhóm cho chuyến.
 * Bảng: trip_staff_assignment
 */
@Entity
@Table(name = "trip_staff_assignment", uniqueConstraints = {
        @UniqueConstraint(name = "uq_trip_staff_assignment_trip_id_staff_id", columnNames = {"trip_id", "staff_id"})})
public class TripStaffAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TransportationTrip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private AppUser staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_role", nullable = false, length = 40)
    private StaffRole assignmentRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private AppUser assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TransportationTrip getTrip() { return trip; }
    public void setTrip(TransportationTrip trip) { this.trip = trip; }

    public AppUser getStaff() { return staff; }
    public void setStaff(AppUser staff) { this.staff = staff; }

    public StaffRole getAssignmentRole() { return assignmentRole; }
    public void setAssignmentRole(StaffRole assignmentRole) { this.assignmentRole = assignmentRole; }

    public AppUser getAssignedBy() { return assignedBy; }
    public void setAssignedBy(AppUser assignedBy) { this.assignedBy = assignedBy; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}
