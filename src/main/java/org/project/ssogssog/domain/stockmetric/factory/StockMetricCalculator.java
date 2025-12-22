package org.project.ssogssog.domain.stockmetric.factory;

import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stockmetric.vo.MetricValues;

/**
 * 순수 도메인 계산용 클래스.
 * - 외부 IO/리포지토리 접근 없음
 * - 입력(DailyPrice, StockFinancial...)을 받아 MetricValues만 계산해서 돌려줌
 */
public class StockMetricCalculator {

    public static MetricValues calculate(
            Stock stock,
            DailyPrice latest,
            StockFinancial current,
            StockFinancial prev,
            StockFinancial prevYearSame
    ) {
        if (latest == null) {
            throw new IllegalArgumentException("최신 시세 데이터(DailyPrice)가 필요합니다. stockCode: " +
                    (stock != null ? stock.getStockCode() : "unknown"));
        }
        if (current == null) {
            throw new IllegalArgumentException("현재 재무 데이터(StockFinancial)가 필요합니다. stockCode: " +
                    (stock != null ? stock.getStockCode() : "unknown"));
        }

        // -----------------------
        // 1. 기본 값들
        // -----------------------
        Integer currentPrice = latest.getClosePrice();
        Long marketCap = latest.getMarketCap();   // 혹은 currentPrice * listedShares 로 재계산

        // 가장 보고서의 재무 값 (연간/분기 구분 없이 "current" 기준)
        Long revenue = current.getRevenue();        // 매출액
        Long netIncome = current.getNetIncome();    // 당기순이익
        Long totalAssets = current.getTotalAssets();            // 자산총계
        Long totalLiabilities = current.getTotalLiabilities();  // 부채총계
        Long totalEquity = current.getTotalEquity();    // 자본총계

        // 발행주식수, 외국인 보유주식수
        Long listedShares = latest.getListedShares();               // 발행주식수
        Long foreignHeldShares = latest.getForeignHeldShares();     // 외국인 보유주식수

        // -----------------------
        // 2. 레벨 지표 (PER, ROE, 순이익률, 부채비율)
        // -----------------------

        // (1) PER = 주가 / EPS,   EPS = netIncome / listedShares
        Double per = null;


        //XXX: PER는 "주가 > 0 & 순이익 > 0"인 경우에만 계산!! (적자는 N/A 취급)
        if (currentPrice != null && currentPrice > 0 &&
                netIncome != null && netIncome > 0 &&
                listedShares != null && listedShares > 0) {

            double eps = (double) netIncome / listedShares;
            if (eps != 0.0) {
                per = safeDivide(currentPrice.doubleValue(), eps);
            }
        }

        // (2) ROE(%) = netIncome / totalEquity * 100
        Double roe = safeDivideToPercent(netIncome, totalEquity);

        // (3) 순이익률(%) = netIncome / revenue * 100
        Double netProfitMargin = safeDivideToPercent(netIncome, revenue);

        // (4) 부채비율(%) = totalLiabilities / totalEquity * 100
        Double debtRatio = safeDivideToPercent(totalLiabilities, totalEquity);

        // -----------------------
        // 3. 성장성 지표 (QoQ / YoY)
        // -----------------------

        // (5) 매출 성장률 QoQ = (curRev / prevRev - 1) * 100
        Double salesGrowthQoQ = null;
        if (prev != null) {
            Long prevRev = prev.getRevenue();
            if (revenue != null && prevRev != null && prevRev != 0) {
                salesGrowthQoQ = ((double) revenue / prevRev - 1.0) * 100.0;
            }
        }

        // (6) 매출 성장률 YoY = (curRev / prevYearRev - 1) * 100
        Double salesGrowthYoY = null;
        if (prevYearSame != null) {
            Long prevYearRev = prevYearSame.getRevenue();
            if (revenue != null && prevYearRev != null && prevYearRev != 0) {
                salesGrowthYoY = ((double) revenue / prevYearRev - 1.0) * 100.0;
            }
        }

        // (7) 순이익 성장률 QoQ = (curNI / prevNI - 1) * 100
        Double netProfitGrowthQoQ = (prev != null)
                ? safeEarningsGrowth(netIncome, prev.getNetIncome())
                : null;

        // (8) 순이익 성장률 YoY = (curNI / prevYearNI - 1) * 100
        Double netProfitGrowthYoY = (prevYearSame != null)
                ? safeEarningsGrowth(netIncome, prevYearSame.getNetIncome())
                : null;


        // -----------------------
        // 4. 배당수익률 (현재는 데이터 소스 없으므로 null 처리)
        // -----------------------
        // ex) dividendYield(%) = DPS / currentPrice * 100
        // StockFinancial에 dividendPerShare 등이 생기면 여기서 계산
        Double dividendYield = null;

        // -----------------------
        // 5. 외국인 보유율
        // -----------------------
        // foreignOwnershipRate(%) = foreignHeldShares / listedShares * 100
        Double foreignOwnershipRate = null;
        if (foreignHeldShares != null &&
                listedShares != null && listedShares != 0) {
            foreignOwnershipRate =
                    (double) foreignHeldShares / listedShares * 100.0;
        }

        // -----------------------
        // 6. 3M / 6M / 12M 수익률
        // -----------------------
        // 기준 가격(3개월 전, 6개월 전, 12개월 전)이 없으므로 null 처리.
        // TODO: calculate 뒷 부분에 price3Mago, price6Mago, price12Mago 추가
        Double return3M = null;
        Double return6M = null;
        Double return12M = null;

        // -----------------------
        // 7. MetricValues로 묶어서 반환
        // -----------------------
        return new MetricValues(
                currentPrice,
                marketCap,
                per,
                roe,
                netProfitMargin,
                debtRatio,
                salesGrowthQoQ,
                salesGrowthYoY,
                netProfitGrowthQoQ,
                netProfitGrowthYoY,
                dividendYield,
                foreignOwnershipRate,
                return3M,
                return6M,
                return12M
        );
    }

    // -----------------------
    // 헬퍼 메서드들
    // -----------------------

    private static Double safeDivide(Double numerator, Double denominator) {
        if (numerator == null || denominator == null || denominator == 0.0) {
            return null;
        }
        return numerator / denominator;
    }

    private static Double safeDivideToPercent(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator == 0L) {
            return null;
        }
        return (double) numerator / denominator * 100.0;
    }

    private static Double safeEarningsGrowth(Long current, Long previous) {
        if (current == null || previous == null || previous <= 0L) {
            // 전기 순이익이 0 이하(적자/손익분기)면 퍼센트 성장률은 의미 없다고 보고 null
            return null;
        }
        return ((double) current / previous - 1.0) * 100.0;
    }
}