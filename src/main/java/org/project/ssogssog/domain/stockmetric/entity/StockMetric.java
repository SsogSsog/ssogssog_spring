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
    @JoinColumn(name = "stock_id")
    private Stock stock;

    // --- [가격 정보] ---
    private Integer currentPrice; // 현재가(종가 기준)
    private Long marketCap;       // 시가총액 (주가 * 발행주식수)

    // --- [수익성 지표] ---
    private Double per;           // PER (주가수익비율)
    private Double roe;           // ROE (자기자본이익률)
    private Double netProfitMargin; // 순이익률

    // --- [안정성 지표] ---
    private Double debtRatio;     // 부채비율

    // --- [성장성 지표] ---
    // 둘 다 QoQ / YoY 분리
    private Double salesGrowthQoQ;      // 매출액 성장률 (직전 분기)
    private Double salesGrowthYoY;      // 매출액 성장률 (전년)
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

}
