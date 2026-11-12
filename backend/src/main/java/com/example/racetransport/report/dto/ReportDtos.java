package com.example.racetransport.report.dto;

import java.math.BigDecimal;
import java.util.List;

/** DTO báo cáo / dashboard. Số liệu tính từ database (dữ liệu demo khi chạy profile dev). */
public final class ReportDtos {

    private ReportDtos() { }

    public record Point(String label, BigDecimal value, BigDecimal value2) {
        public static Point of(String label, long v) {
            return new Point(label, BigDecimal.valueOf(v), null);
        }

        public static Point of(String label, BigDecimal v) {
            return new Point(label, v, null);
        }

        public static Point of(String label, BigDecimal v, BigDecimal v2) {
            return new Point(label, v, v2);
        }
    }

    public record RevenueReport(String currency, BigDecimal invoicedTotal, BigDecimal collectedTotal,
                                BigDecimal outstandingTotal, BigDecimal acceptedQuoteTotal, long invoiceCount,
                                long paidInvoiceCount, List<Point> monthly, List<Point> costBreakdown,
                                BigDecimal claimSettledTotal, String note) { }

    public record TransportReport(long totalTrips, long activeTrips, long deliveredTrips, long cancelledTrips,
                                  long horsesTransported, List<Point> tripsByStatus, List<Point> tripsByMonth,
                                  List<Point> requestsByStatus, List<Point> topRoutes, List<Point> byTransportMode) { }

    public record IncidentReport(long total, long open, long critical, BigDecimal avgResolutionHours,
                                 List<Point> bySeverity, List<Point> byType, List<Point> byMonth,
                                 List<Point> byDecision, long totalDelayMinutes) { }

    public record ClaimReport(long total, long open, BigDecimal claimedTotal, BigDecimal approvedTotal,
                              BigDecimal settledTotal, BigDecimal approvalRatioPercent, List<Point> byStatus,
                              List<Point> byType) { }

    public record Kpi(String key, String label, BigDecimal value, String unit, String description) { }

    public record KpiReport(List<Kpi> kpis, String note) { }

    // --------------------------------------------------------------- dashboard
    public record Card(String key, String label, String value, String hint, String tone, String link) { }

    public record Item(Long id, String title, String subtitle, String status, String meta, String link) { }

    public record Section(String key, String title, List<Item> items, String emptyText, String link) { }

    public record DashboardResponse(String role, String greeting, List<Card> cards, List<Section> sections,
                                    List<Point> chart, String chartTitle) { }
}
