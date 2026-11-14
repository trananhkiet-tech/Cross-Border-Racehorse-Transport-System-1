package com.example.racetransport.staffleave.repository;

import com.example.racetransport.staffleave.entity.LeaveStatus;
import com.example.racetransport.staffleave.entity.StaffLeave;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Long> {

    List<StaffLeave> findByStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(LeaveStatus status, LocalDate from);

    List<StaffLeave> findByStaffIdOrderByStartDateDesc(Long staffId);

    /** Có kỳ nghỉ còn hiệu lực trùng khoảng ngày. */
    @Query("""
            select case when count(l) > 0 then true else false end from StaffLeave l
            where l.staffId = :staffId and l.status = com.example.racetransport.staffleave.entity.LeaveStatus.ACTIVE
              and l.startDate <= :end and l.endDate >= :start
            """)
    boolean existsOverlap(@Param("staffId") Long staffId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
