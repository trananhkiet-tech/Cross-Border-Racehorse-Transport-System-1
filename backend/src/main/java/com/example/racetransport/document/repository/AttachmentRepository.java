package com.example.racetransport.document.repository;

import com.example.racetransport.document.entity.Attachment;
import com.example.racetransport.document.entity.AttachmentRefType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @EntityGraph(attributePaths = "file")
    List<Attachment> findByRefTypeAndRefIdOrderByIdAsc(AttachmentRefType refType, Long refId);

    @EntityGraph(attributePaths = "file")
    List<Attachment> findByRefTypeAndRefIdIn(AttachmentRefType refType, Collection<Long> refIds);
}
