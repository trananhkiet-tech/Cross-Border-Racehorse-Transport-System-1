package com.example.racetransport.requestqa.repository;

import com.example.racetransport.requestqa.entity.RequestQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestQuestionRepository extends JpaRepository<RequestQuestion, Long> {

    List<RequestQuestion> findByRequestIdOrderByCreatedAtAsc(Long requestId);

    List<RequestQuestion> findByAnswerIsNullOrderByCreatedAtAsc();
}
