package com.example.racetransport.transportationrequest.repository;

import com.example.racetransport.transportationrequest.entity.RequestStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestStatusHistoryRepository extends JpaRepository<RequestStatusHistory, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<RequestStatusHistory> findByRequestIdOrderByChangedAtAscIdAsc(Long requestId);
}
