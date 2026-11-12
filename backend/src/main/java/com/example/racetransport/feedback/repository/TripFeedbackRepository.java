package com.example.racetransport.feedback.repository;

import com.example.racetransport.feedback.entity.TripFeedback;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripFeedbackRepository extends JpaRepository<TripFeedback, Long> {

    Optional<TripFeedback> findByTripId(Long tripId);

    List<TripFeedback> findByTripIdIn(Collection<Long> tripIds);
}
