package org.project.ssogssog.domain.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_financial",
        uniqueConstraints = {
                // 한 종목의 특정 년도/분기/재무기준 데이터는 유일해야 함
                @UniqueConstraint(
                        name = "uk_stock_financial_detail",
                        columnNames = {"stock_id", "year", "quarter", "is_consolidated"}
                )
        }
)
// TODO 조회가 대부분인 테이블이므로 @index 고려해보기
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFinancial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 종목의 재무인지 연결 (Stock의 stockCode와 매핑)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // [중요] 연결재무제표 여부 (true: 연결, false: 별도)
    // 스크리너에서는 주로 where isConsolidated = true 로 조회함
    @Column(name = "is_consolidated", nullable = false)
    private boolean isConsolidated;

    private Integer year;     // 제무재표 년도
    private String quarter;   // 1Q, 2Q, 3Q, 4Q

    // 손익계산서 (성장성, 수익성)
    private Long revenue;          // 매출액
    private Long operatingProfit;  // 영업이익
    private Long netIncome;        // 당기순이익

    // 재무상태표 (안정성)
    private Long totalAssets;      // 자산총계
    private Long totalLiabilities; // 부채총계
    private Long totalEquity;      // 자본총계

    // 현금흐름 핵심
    private Long operatingCashFlow; // 영업활동 현금흐름
    private Long freeCashFlow;      // 자유현금흐름 (optional: 계산 후 저장)

    // 발행주식수 (EPS/BPS/PER, PBR 계산용)
    private Long sharesOutstanding; // 발행주식수
}
