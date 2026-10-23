package com.example.racetransport.common.entity;

import jakarta.persistence.*;

/**
 * Bộ đếm sinh mã nghiệp vụ (REQ-2026-0159…) có khóa bi quan.
 * Bảng: code_sequence
 */
@Entity
@Table(name = "code_sequence")
public class CodeSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** VD: REQ-2026 */
    @Column(name = "seq_key", nullable = false, unique = true, length = 30)
    private String seqKey;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSeqKey() { return seqKey; }
    public void setSeqKey(String seqKey) { this.seqKey = seqKey; }

    public Long getNextValue() { return nextValue; }
    public void setNextValue(Long nextValue) { this.nextValue = nextValue; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
