package com.example.racetransport.quotation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Dòng chi phí của báo giá.
 * Bảng: quote_item
 */
@Entity
@Table(name = "quote_item")
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private CostCategory category;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    /** Backend tính; DISCOUNT luôn âm */
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** Số ước tính ban đầu để so sánh */
    @Column(name = "estimated_amount", precision = 18, scale = 2)
    private BigDecimal estimatedAmount;

    /** Manager đã xác nhận chi phí thực */
    @Column(name = "confirmed", nullable = false)
    private Boolean confirmed = Boolean.FALSE;

    @Column(name = "source", length = 150)
    private String source;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }

    public CostCategory getCategory() { return category; }
    public void setCategory(CostCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getEstimatedAmount() { return estimatedAmount; }
    public void setEstimatedAmount(BigDecimal estimatedAmount) { this.estimatedAmount = estimatedAmount; }

    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
