package com.example.racetransport.common.repository;

import com.example.racetransport.common.entity.CodeSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, Long> {

    /** Khóa bi quan (UPDLOCK trên SQL Server) để hai giao dịch không lấy trùng số. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodeSequence> findBySeqKey(String seqKey);
}
