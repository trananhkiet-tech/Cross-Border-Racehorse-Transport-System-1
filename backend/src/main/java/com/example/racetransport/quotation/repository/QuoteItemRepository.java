package com.example.racetransport.quotation.repository;

import com.example.racetransport.quotation.entity.QuoteItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long> {

    List<QuoteItem> findByQuoteIdOrderBySortOrderAscIdAsc(Long quoteId);

    @Modifying
    @Query("delete from QuoteItem i where i.quote.id = :quoteId")
    void deleteByQuoteId(@Param("quoteId") Long quoteId);

    /** Tổng chi phí theo hạng mục của các báo giá cuối đã được chấp nhận ([category, sum]). */
    @Query("""
            select i.category, sum(i.amount) from QuoteItem i
            where i.quote.status = com.example.racetransport.quotation.entity.QuoteStatus.ACCEPTED
              and i.quote.quoteType = com.example.racetransport.quotation.entity.QuoteType.FINAL
            group by i.category
            """)
    List<Object[]> sumAcceptedByCategory();
}
