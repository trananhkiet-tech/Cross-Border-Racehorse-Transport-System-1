package com.example.racetransport.document.repository;

import com.example.racetransport.document.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
