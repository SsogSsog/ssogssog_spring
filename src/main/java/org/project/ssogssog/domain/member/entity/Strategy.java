package org.project.ssogssog.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.domain.member.entity.range.GrowthRangeCondition;
import org.project.ssogssog.domain.member.entity.range.RangeCondition;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;

@Entity
@Table(name = "strategy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String strategyName; // 전략 이름


    @Enumerated(EnumType.STRING)
    private StockPriceRange stockPriceRange; // 현재가

    @Enumerated(EnumType.STRING)
    private MarketCapBucket marketCapBucket; // 시가총액


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_per")),
            @AttributeOverride(name = "max", column = @Column(name = "max_per"))
    })
    private RangeCondition per; // PER

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_roe")),
            @AttributeOverride(name = "max", column = @Column(name = "max_roe"))
    })
    private RangeCondition roe; // ROE

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_net_profit_margin")),
            @AttributeOverride(name = "max", column = @Column(name = "max_net_profit_margin"))
    })
    private RangeCondition netProfitMargin; // 순이익률

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_operating_profit_margin")),
            @AttributeOverride(name = "max", column = @Column(name = "max_operating_profit_margin"))
    })
    private RangeCondition operatingProfitMargin; // 영업이익률


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_debt_ratio")),
            @AttributeOverride(name = "max", column = @Column(name = "max_debt_ratio"))
    })
    private RangeCondition debtRatio; // 부채비율


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_sales_growth_qoq")),
            @AttributeOverride(name = "max", column = @Column(name = "max_sales_growth_qoq")),
            @AttributeOverride(name = "basePeriod", column = @Column(name = "sales_growth_qoq_base_period"))
    })
    private GrowthRangeCondition salesGrowthQoQ; // 매출액 성장률 (직전 분기)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_sales_growth_yoy")),
            @AttributeOverride(name = "max", column = @Column(name = "max_sales_growth_yoy")),
            @AttributeOverride(name = "basePeriod", column = @Column(name = "sales_growth_yoy_base_period"))
    })
    private GrowthRangeCondition salesGrowthYoY; // 매출액 성장률 (전년 동기)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_net_profit_growth_qoq")),
            @AttributeOverride(name = "max", column = @Column(name = "max_net_profit_growth_qoq")),
            @AttributeOverride(name = "basePeriod", column = @Column(name = "net_profit_growth_qoq_base_period"))
    })
    private GrowthRangeCondition netProfitGrowthQoQ; // 순이익 성장률 (직전 분기)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_net_profit_growth_yoy")),
            @AttributeOverride(name = "max", column = @Column(name = "max_net_profit_growth_yoy")),
            @AttributeOverride(name = "basePeriod", column = @Column(name = "net_profit_growth_yoy_base_period"))
    })
    private GrowthRangeCondition netProfitGrowthYoY; // 순이익 성장률 (전년 동기)


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_dividend_yield")),
            @AttributeOverride(name = "max", column = @Column(name = "max_dividend_yield"))
    })
    private RangeCondition dividendYield; // 배당수익률


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_foreign_ownership_rate")),
            @AttributeOverride(name = "max", column = @Column(name = "max_foreign_ownership_rate"))
    })
    private RangeCondition foreignOwnershipRate; // 외국인 보유율


    /**
     * 적용된 조건의 개수를 반환
     */
    public int getAppliedConditionsCount() {
        int count = 0;

        if (stockPriceRange != null) count++;
        if (marketCapBucket != null) count++;
        if (per != null && per.isApplied()) count++;
        if (roe != null && roe.isApplied()) count++;
        if (netProfitMargin != null && netProfitMargin.isApplied()) count++;
        if (operatingProfitMargin != null && operatingProfitMargin.isApplied()) count++;
        if (debtRatio != null && debtRatio.isApplied()) count++;
        if (salesGrowthQoQ != null && salesGrowthQoQ.isApplied()) count++;
        if (salesGrowthYoY != null && salesGrowthYoY.isApplied()) count++;
        if (netProfitGrowthQoQ != null && netProfitGrowthQoQ.isApplied()) count++;
        if (netProfitGrowthYoY != null && netProfitGrowthYoY.isApplied()) count++;
        if (dividendYield != null && dividendYield.isApplied()) count++;
        if (foreignOwnershipRate != null && foreignOwnershipRate.isApplied()) count++;

        return count;
    }

    /**
     * 주식이 이 전략의 모든 조건을 만족하는지 확인
     */
    // TODO NPE 검사
    public boolean matchesStock(StockMetric stock) {

        try{
            if (stockPriceRange != null && !matchesStockPriceRange(stock.getCurrentPrice())) return false;
            if (marketCapBucket != null && !matchesMarketCapBucket(stock.getMarketCap())) return false;
            if (per != null && !per.matches(stock.getPer())) return false;
            if (roe != null && !roe.matches(stock.getRoe())) return false;
            if (netProfitMargin != null && !netProfitMargin.matches(stock.getNetProfitMargin())) return false;
            if (operatingProfitMargin != null && !operatingProfitMargin.matches(stock.getOperatingProfitMargin())) return false;
            if (debtRatio != null && !debtRatio.matches(stock.getDebtRatio())) return false;
            if (salesGrowthQoQ != null && !salesGrowthQoQ.matches(stock.getSalesGrowthQoQ())) return false;
            if (salesGrowthYoY != null && !salesGrowthYoY.matches(stock.getSalesGrowthYoY())) return false;
            if (netProfitGrowthQoQ != null && !netProfitGrowthQoQ.matches(stock.getNetProfitGrowthQoQ())) return false;
            if (netProfitGrowthYoY != null && !netProfitGrowthYoY.matches(stock.getNetProfitGrowthYoY())) return false;
            if (dividendYield != null && !dividendYield.matches(stock.getDividendYield())) return false;
            if (foreignOwnershipRate != null && !foreignOwnershipRate.matches(stock.getForeignOwnershipRate())) return false;
        }catch(Exception e){
            return false;
        }

        return true;
    }

    private boolean matchesStockPriceRange(Integer currentPrice) {
        if (currentPrice == null) return false;
        Integer min = StockPriceRange.minPrice(stockPriceRange);
        Integer max = StockPriceRange.maxPrice(stockPriceRange);
        if (min != null && currentPrice < min) return false;
        if (max != null && currentPrice > max) return false;
        return true;
    }

    private boolean matchesMarketCapBucket(Long marketCap) {
        if (marketCap == null) return false;
        Long min = MarketCapBucket.minPrice(marketCapBucket);
        Long max = MarketCapBucket.maxPrice(marketCapBucket);
        if (min != null && marketCap < min) return false;
        if (max != null && marketCap > max) return false;
        return true;
    }

}
