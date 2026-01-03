package org.project.ssogssog.domain.stockmetric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.domain.stock.entity.Stock;

import java.time.LocalDate;

@Entity
@Table(name = "stock_metric")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 종목의 지표인지 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", unique = true)
    private Stock stock;

    // --- [가격 정보] ---
    private Integer currentPrice; // 현재가(종가 기준)
    private Long marketCap;       // 시가총액 (주가 * 발행주식수)

    // --- [수익성 지표] ---
    private Double per;           // PER (주가수익비율)
    private Double roe;           // ROE (자기자본이익률)
    private Double netProfitMargin; // 순이익률
    private Double operatingProfitMargin; // 영업이익률

    // --- [안정성 지표] ---
    private Double debtRatio;     // 부채비율

    // --- [성장성 지표] ---
    // 둘 다 QoQ / YoY 분리
    // QoQ 정의:
    // - 2Q: (1~6 vs 1~3)
    // - 3Q: (1~9 vs 1~6)
    // - 4Q: (1~12 vs 1~9)
    // - 1Q: (당기 1Q 누적 vs 전년도 4Q(연간))  <-- 비표준 정의, 해석 주의
    private Double salesGrowthQoQ;      // 매출액 성장률 (직전 분기)
    private Double salesGrowthYoY;      // 매출액 성장률 (전년)

    // 직전 재무보고서 대비 누적 기준 순이익 성장률 (1Q는 전년도 사업보고서 대비)
    private Double netProfitGrowthQoQ;  // 순이익 성장률 (직전 분기)
    private Double netProfitGrowthYoY;  // 순수익 성장률 (작년)


    // --- [배당] ---
    private Double dividendYield; // 배당수익률

    // --- [수급/구조] ---
    private Double foreignOwnershipRate; // 외국인 보유율 (%)

    // --- [주가 수익률] ---
    private Double return3M;
    private Double return6M;
    private Double return12M;

    // 언제 기준 데이터인지 (데이터 갱신일)
    private LocalDate calculatedAt;



    /**
    비즈니스 로직
     */
    public void updateAll(
            Integer currentPrice,
            Long marketCap,
            Double per,
            Double roe,
            Double netProfitMargin,
            Double debtRatio,
            Double operatingProfitMargin,
            Double salesGrowthQoQ,
            Double salesGrowthYoY,
            Double netProfitGrowthQoQ,
            Double netProfitGrowthYoY,
            Double dividendYield,
            Double foreignOwnershipRate,
            Double return3M,
            Double return6M,
            Double return12M
    ) {
        this.currentPrice = currentPrice;
        this.marketCap = marketCap;

        this.per = per;
        this.roe = roe;
        this.netProfitMargin = netProfitMargin;
        this.debtRatio = debtRatio;
        this.operatingProfitMargin = operatingProfitMargin;

        this.salesGrowthQoQ = salesGrowthQoQ;
        this.salesGrowthYoY = salesGrowthYoY;
        this.netProfitGrowthQoQ = netProfitGrowthQoQ;
        this.netProfitGrowthYoY = netProfitGrowthYoY;

        this.dividendYield = dividendYield;
        this.foreignOwnershipRate = foreignOwnershipRate;

        this.return3M = return3M;
        this.return6M = return6M;
        this.return12M = return12M;

        // 지표 재계산 시점
        this.calculatedAt = LocalDate.now();
    }

}
