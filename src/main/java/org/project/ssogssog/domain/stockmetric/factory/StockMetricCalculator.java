package org.project.ssogssog.domain.stockmetric.factory;

import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stockmetric.vo.MetricValues;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
            StockFinancial prevPrev,      // QoQ 계산용 직전직전 분기
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
        Long operatingProfit = current.getOperatingProfit(); // 영업이익
        Long totalAssets = current.getTotalAssets();            // 자산총계
        Long totalLiabilities = current.getTotalLiabilities();  // 부채총계
        Long totalEquity = current.getTotalEquity();    // 자본총계

        // 발행주식수, 외국인 보유주식수
        Long listedShares = latest.getListedShares();               // 발행주식수
        Long foreignHeldShares = latest.getForeignHeldShares();     // 외국인 보유주식수

        // -----------------------
        // 2. 레벨 지표 (PER, ROE, 순이익률, 부채비율)
        // -----------------------

        // (1) PER = 주가 / EPS,   EPS = 연율화된 netIncome / listedShares
        Double per = null;

        // 연율화된 순이익 계산 (분기 누적 → 연간 추정)
        String quarter = current.getQuarter();
        Long annualizedNetIncome = annualizeNetIncome(netIncome, quarter);

        //XXX: PER는 "주가 > 0 & 순이익 > 0"인 경우에만 계산!! (적자는 N/A 취급)
        if (currentPrice != null && currentPrice > 0 &&
                annualizedNetIncome != null && annualizedNetIncome > 0 &&
                listedShares != null && listedShares > 0) {

            double eps = (double) annualizedNetIncome / listedShares;
            if (eps != 0.0) {
                per = safeDivide(currentPrice.doubleValue(), eps);
            }
        }

        // (2) ROE(%) = 연율화된 netIncome / totalEquity * 100
        Double roe = safeDivideToPercent(annualizedNetIncome, totalEquity);

        // (3) 순이익률(%) = netIncome / revenue * 100
        Double netProfitMargin = safeDivideToPercent(netIncome, revenue);

        // (4) 부채비율(%) = totalLiabilities / totalEquity * 100
        Double debtRatio = safeDivideToPercent(totalLiabilities, totalEquity);

        // (5) 영업이익률(%) = operatingProfit / revenue * 100
        Double operatingProfitMargin = safeDivideToPercent(operatingProfit, revenue);


        // -----------------------
        // 3. 성장성 지표 (QoQ / YoY)
        // -----------------------

        // QoQ: 단독 분기 값 비교 (누적이 아닌 해당 분기만의 실적)
        // - current 단독 = current 누적 - prev 누적 (1Q는 그대로)
        // - prev 단독 = prev 누적 - prevPrev 누적 (1Q는 그대로)

        // (5) 매출 성장률 QoQ = (current 단독 매출 / prev 단독 매출 - 1) * 100
        Double salesGrowthQoQ = null;
        if (prev != null) {
            Long currentStandaloneRev = getStandaloneQuarterValue(revenue, prev.getRevenue(), quarter);
            Long prevStandaloneRev = getStandaloneQuarterValue(
                    prev.getRevenue(),
                    prevPrev != null ? prevPrev.getRevenue() : null,
                    prev.getQuarter()
            );

            if (currentStandaloneRev != null && prevStandaloneRev != null && prevStandaloneRev != 0) {
                salesGrowthQoQ = ((double) currentStandaloneRev / prevStandaloneRev - 1.0) * 100.0;
            }
        }

        // (6) 매출 성장률 YoY = (당분기 누적 / 전년 동기 누적 - 1) * 100
        // YoY는 누적 비교가 맞음 (동일 기간 비교)
        Double salesGrowthYoY = null;
        if (prevYearSame != null) {
            Long prevYearRev = prevYearSame.getRevenue();
            if (revenue != null && prevYearRev != null && prevYearRev != 0) {
                salesGrowthYoY = ((double) revenue / prevYearRev - 1.0) * 100.0;
            }
        }

        // (7) 순이익 성장률 QoQ = (current 단독 순이익 / prev 단독 순이익 - 1) * 100
        Double netProfitGrowthQoQ = null;
        if (prev != null) {
            Long currentStandaloneNI = getStandaloneQuarterValue(netIncome, prev.getNetIncome(), quarter);
            Long prevStandaloneNI = getStandaloneQuarterValue(
                    prev.getNetIncome(),
                    prevPrev != null ? prevPrev.getNetIncome() : null,
                    prev.getQuarter()
            );

            netProfitGrowthQoQ = safeEarningsGrowth(currentStandaloneNI, prevStandaloneNI);
        }

        // (8) 순이익 성장률 YoY = (당분기 누적 / 전년 동기 누적 - 1) * 100
        // YoY는 누적 비교가 맞음 (동일 기간 비교)
        Double netProfitGrowthYoY = (prevYearSame != null)
                ? safeEarningsGrowth(netIncome, prevYearSame.getNetIncome())
                : null;


        // -----------------------
        // 4. 배당수익률
        // -----------------------
        // ex) dividendYield(%) = DPS / currentPrice * 100
        Double dividendYield = calcDividendYield(currentPrice, stock != null ? stock.getLastDps() : null);

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
                operatingProfitMargin,
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


    // 배당 수익률 계산 로직
    private static Double calcDividendYield(Integer currentPrice, Integer lastDps) {
        if (currentPrice == null || currentPrice <= 0) return null;     // 가격 데이터 이상/없음
        if (lastDps == null) return null;                                // DPS 미수집
        if (lastDps <= 0) return 0.0;                                    // 배당 없음

        double raw = (lastDps * 100.0) / currentPrice;                   // (%)
        return BigDecimal.valueOf(raw)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // -----------------------
    // 헬퍼 메서드들
    // -----------------------

    /**
     * 분기 누적 데이터를 연간 기준으로 연율화하는 계수 반환
     * - 1Q: 3개월 → 12개월 (×4)
     * - 2Q: 6개월 → 12개월 (×2)
     * - 3Q: 9개월 → 12개월 (×1.333...)
     * - 4Q: 12개월 → 12개월 (×1)
     */
    private static double getAnnualizationFactor(String quarter) {
        return switch (quarter) {
            case "1Q" -> 4.0;
            case "2Q" -> 2.0;
            case "3Q" -> 12.0 / 9.0;  // ≈ 1.333
            case "4Q" -> 1.0;
            default -> 1.0;
        };
    }

    /**
     * 누적 순이익을 연율화하여 반환
     */
    private static Long annualizeNetIncome(Long netIncome, String quarter) {
        if (netIncome == null || quarter == null) {
            return null;
        }
        double factor = getAnnualizationFactor(quarter);
        return Math.round(netIncome * factor);
    }

    /**
     * 단독 분기 값 계산 (누적 데이터에서 해당 분기만 추출)
     * - 1Q: 누적 = 단독 (그대로 반환)
     * - 2Q/3Q/4Q: 당분기 누적 - 직전 분기 누적
     *
     * @param currentValue 현재 분기 누적값
     * @param prevValue    직전 분기 누적값 (1Q인 경우 무시됨)
     * @param quarter      현재 분기 ("1Q", "2Q", "3Q", "4Q")
     * @return 단독 분기 값
     */
    private static Long getStandaloneQuarterValue(Long currentValue, Long prevValue, String quarter) {
        if (currentValue == null) {
            return null;
        }

        // 1Q는 누적 = 단독
        if ("1Q".equals(quarter)) {
            return currentValue;
        }

        // 2Q/3Q/4Q는 직전 분기 누적값이 필요
        if (prevValue == null) {
            return null;
        }

        return currentValue - prevValue;
    }

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