package com.example.racetransport.common.service;

import com.example.racetransport.common.entity.CodeSequence;
import com.example.racetransport.common.repository.CodeSequenceRepository;
import java.time.Clock;
import java.time.Year;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sinh mã nghiệp vụ dễ đọc: REQ-2026-0159, TRIP-2026-0095, INV-2026-0012...
 * Bộ đếm lưu trong bảng code_sequence theo từng tiền tố + năm, có khóa ghi.
 */
@Service
public class CodeGenerator {

    public static final String REQUEST = "REQ";
    public static final String TRIP = "TRIP";
    public static final String QUOTE = "QT";
    public static final String INVOICE = "INV";
    public static final String PAYMENT = "PAY";
    public static final String INCIDENT = "INC";
    public static final String HANDOVER = "HO";
    public static final String CLAIM = "CLM";
    public static final String SETTLEMENT = "STL";
    public static final String ROUTE = "RT";

    private final CodeSequenceRepository repository;
    private final Clock clock;

    public CodeGenerator(CodeSequenceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(String prefix) {
        int year = Year.now(clock).getValue();
        String key = prefix + "-" + year;
        CodeSequence seq = repository.findBySeqKey(key).orElseGet(() -> {
            CodeSequence created = new CodeSequence();
            created.setSeqKey(key);
            created.setNextValue(1L);
            return created;
        });
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        repository.save(seq);
        return String.format("%s-%04d", key, value);
    }

    /** Dùng khi seed dữ liệu: bảo đảm bộ đếm luôn lớn hơn số đã dùng. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void ensureAtLeast(String prefix, int year, long nextValue) {
        String key = prefix + "-" + year;
        CodeSequence seq = repository.findBySeqKey(key).orElseGet(() -> {
            CodeSequence created = new CodeSequence();
            created.setSeqKey(key);
            created.setNextValue(1L);
            return created;
        });
        if (seq.getNextValue() < nextValue) {
            seq.setNextValue(nextValue);
        }
        repository.save(seq);
    }
}
