package com.example.racetransport.tripexpense.repository;

import com.example.racetransport.tripexpense.entity.ExpenseStatus;
import com.example.racetransport.tripexpense.entity.TripExpense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripExpenseRepository extends JpaRepository<TripExpense, Long> {

    List<TripExpense> findByStaffIdOrderByCreatedAtDesc(Long staffId);

    List<TripExpense> findByStatusOrderByCreatedAtAsc(ExpenseStatus status);

    List<TripExpense> findAllByOrderByCreatedAtDesc();
}
