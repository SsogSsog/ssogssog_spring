package org.project.ssogssog.domain.stockmetric.factory;

import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stockmetric.vo.MetricValues;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 주식 지표 계산기 (TTM 기반)
 *
 * OpenDART 데이터 특성:
 * - 1Q, 2Q, 3Q: 해당 분기의 개별(단독) 실적
 * - 4Q: 연간 누적 실적 (1Q+2Q+3Q+4Q)
 *
 * TTM(Trailing Twelve Months) 계산:
 * - 최근 12개월 실적을 합산하여 연간 기준 지표 산출
 */
@Slf4j
public class StockMetricCalculator {

    /**
     * 메트릭 계산 메서드
     *
     * @param stock             종목 정보
     * @param latest            최신 일별 시세
     * @param currentYear       올해 분기별 재무 데이터 (key: "1Q", "2Q", "3Q", "4Q")
     * @param lastYear          작년 분기별 재무 데이터 (key: "1Q", "2Q", "3Q", "4Q")
     * @param currentQuarter    현재 기준 분기 (예: "3Q")
     */
    public static MetricValues calculate(
            Stock stock,
            DailyPrice latest,
            Map<String, StockFinancial> currentYear,
            Map<String, StockFinancial> lastYear,
            String currentQuarter
    ) {
        String stockCode = stock != null ? stock.getStockCode() : "unknown";

        // 필수 데이터 검증
        if (latest == null) {
            log.warn("[{}] 계산 불가 - DailyPrice 없음", stockCode);
            return null;
        }
        if (currentYear == null || currentYear.isEmpty()) {
            log.warn("[{}] 계산 불가 - 올해 재무 데이터 없음", stockCode);
            return null;
        }

        StockFinancial current = currentYear.get(currentQuarter);
        if (current == null) {
            log.warn("[{}] 계산 불가 - 현재 분기({}) 데이터 없음", stockCode, currentQuarter);
            return null;
        }

        // -----------------------
        // 1. 기본 값들
        // -----------------------
        Integer currentPrice = latest.getClosePrice();
        Long marketCap = latest.getMarketCap();
        Long listedShares = latest.getListedShares();
        Long foreignHeldShares = latest.getForeignHeldShares();

        Long totalEquity = current.getTotalEquity();
        Long totalLiabilities = current.getTotalLiabilities();

        log.debug("[{}] 기본값 - currentPrice: {}, marketCap: {}, listedShares: {}",
                stockCode, currentPrice, marketCap, listedShares);

        // -----------------------
        // 2. TTM 계산 (PER, ROE용)
        // -----------------------
        Long ttmNetIncome = calculateTTM(
                currentYear, lastYear, currentQuarter,
                StockFinancial::getNetIncome, stockCode, "순이익"
        );

        Long ttmRevenue = calculateTTM(
                currentYear, lastYear, currentQuarter,
                StockFinancial::getRevenue, stockCode, "매출액"
        );

        Long ttmOperatingProfit = calculateTTM(
                currentYear, lastYear, currentQuarter,
                StockFinancial::getOperatingProfit, stockCode, "영업이익"
        );

        log.debug("[{}] TTM 결과 - 순이익: {}, 매출액: {}, 영업이익: {}",
                stockCode, ttmNetIncome, ttmRevenue, ttmOperatingProfit);

        // -----------------------
        // 3. PER 계산
        // -----------------------
        Double per = null;
        Double eps = null;
        if (currentPrice != null && currentPrice > 0 &&
                ttmNetIncome != null && ttmNetIncome > 0 &&
                listedShares != null && listedShares > 0) {

            eps = (double) ttmNetIncome / listedShares;
            per = currentPrice / eps;

            log.debug("[{}] PER 계산 - EPS: {}, PER: {}", stockCode, eps, per);
        }

        // -----------------------
        // 4. PBR 계산 (시가총액 / 자본총계)
        // -----------------------
        Double pbr = safeDivide(marketCap, totalEquity);
        log.debug("[{}] PBR: {}", stockCode, pbr);

        // -----------------------
        // 5. ROE 계산 (TTM 순이익 / 자본총계)
        // -----------------------
        Double roe = safeDivideToPercent(ttmNetIncome, totalEquity);
        log.debug("[{}] ROE: {}%", stockCode, roe);

        // -----------------------
        // 6. 비율 지표 (TTM 기준)
        // -----------------------
        Double netProfitMargin = safeDivideToPercent(ttmNetIncome, ttmRevenue);
        Double operatingProfitMargin = safeDivideToPercent(ttmOperatingProfit, ttmRevenue);
        Double debtRatio = safeDivideToPercent(totalLiabilities, totalEquity);

        log.debug("[{}] 비율지표 - 순이익률: {}%, 영업이익률: {}%, 부채비율: {}%",
                stockCode, netProfitMargin, operatingProfitMargin, debtRatio);

        // -----------------------
        // 7. 성장률 (QoQ, YoY)
        // -----------------------
        // QoQ: 현재 분기 vs 직전 분기 (개별 실적 비교)
        // 주의: 1Q의 직전 분기는 작년 4Q이므로 lastYear에서 조회해야 함
        String prevQuarter = getPrevQuarter(currentQuarter);
        boolean prevQuarterIsLastYear = "1Q".equals(currentQuarter);  // 1Q의 직전은 작년 4Q

        Long currentDiscreteRevenue = getDiscreteValue(currentYear, lastYear, currentQuarter, StockFinancial::getRevenue);
        Long prevDiscreteRevenue = prevQuarterIsLastYear
                ? getDiscreteValue(lastYear, null, prevQuarter, StockFinancial::getRevenue)
                : getDiscreteValue(currentYear, lastYear, prevQuarter, StockFinancial::getRevenue);
        Double salesGrowthQoQ = safeGrowthRate(currentDiscreteRevenue, prevDiscreteRevenue);

        Long currentDiscreteNI = getDiscreteValue(currentYear, lastYear, currentQuarter, StockFinancial::getNetIncome);
        Long prevDiscreteNI = prevQuarterIsLastYear
                ? getDiscreteValue(lastYear, null, prevQuarter, StockFinancial::getNetIncome)
                : getDiscreteValue(currentYear, lastYear, prevQuarter, StockFinancial::getNetIncome);
        Double netProfitGrowthQoQ = safeGrowthRate(currentDiscreteNI, prevDiscreteNI);

        // YoY: 현재 분기 vs 작년 동일 분기
        Long lastYearSameQuarterRevenue = getDiscreteValue(lastYear, null, currentQuarter, StockFinancial::getRevenue);
        Double salesGrowthYoY = safeGrowthRate(currentDiscreteRevenue, lastYearSameQuarterRevenue);

        Long lastYearSameQuarterNI = getDiscreteValue(lastYear, null, currentQuarter, StockFinancial::getNetIncome);
        Double netProfitGrowthYoY = safeGrowthRate(currentDiscreteNI, lastYearSameQuarterNI);

        log.debug("[{}] 성장률 - 매출QoQ: {}%, 매출YoY: {}%, 순이익QoQ: {}%, 순이익YoY: {}%",
                stockCode, salesGrowthQoQ, salesGrowthYoY, netProfitGrowthQoQ, netProfitGrowthYoY);

        // -----------------------
        // 8. 기타 지표
        // -----------------------
        Double dividendYield = calcDividendYield(currentPrice, stock != null ? stock.getLastDps() : null);

        Double foreignOwnershipRate = null;
        if (foreignHeldShares != null && listedShares != null && listedShares > 0) {
            foreignOwnershipRate = (double) foreignHeldShares / listedShares * 100.0;
        }

        log.debug("[{}] 기타 - 배당수익률: {}%, 외국인보유율: {}%",
                stockCode, dividendYield, foreignOwnershipRate);

        // -----------------------
        // 9. 결과 반환
        // -----------------------
        return new MetricValues(
                currentPrice,
                marketCap,
                per,
                pbr,
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
                null, // return3M
                null, // return6M
                null  // return12M
        );
    }

    // =========================================================================
    // TTM 계산 핵심 로직
    // =========================================================================

    /**
     * TTM(최근 12개월) 값 계산
     *
     * 분기별 TTM 구성:
     * - 1Q: 올해1Q + 작년4Q단독 + 작년3Q + 작년2Q
     * - 2Q: 올해2Q + 올해1Q + 작년4Q단독 + 작년3Q
     * - 3Q: 올해3Q + 올해2Q + 올해1Q + 작년4Q단독
     * - 4Q: 4Q 자체가 연간 누적이므로 그대로 사용
     */
    private static Long calculateTTM(
            Map<String, StockFinancial> currentYear,
            Map<String, StockFinancial> lastYear,
            String currentQuarter,
            java.util.function.Function<StockFinancial, Long> valueExtractor,
            String stockCode,
            String fieldName
    ) {
        // 4Q인 경우: 연간 누적 그대로 반환
        if ("4Q".equals(currentQuarter)) {
            StockFinancial q4 = currentYear.get("4Q");
            Long value = q4 != null ? valueExtractor.apply(q4) : null;
            log.debug("[{}] TTM({}) - 4Q 연간누적 사용: {}", stockCode, fieldName, value);
            return value;
        }

        // 1Q, 2Q, 3Q인 경우: TTM 합산 필요
        Long ttm = 0L;
        boolean hasValidData = false;

        // Step 1: 올해 분기 합산 (개별 실적)
        switch (currentQuarter) {
            case "3Q": {
                Long q3 = safeGetValue(currentYear, "3Q", valueExtractor);
                Long q2 = safeGetValue(currentYear, "2Q", valueExtractor);
                Long q1 = safeGetValue(currentYear, "1Q", valueExtractor);
                if (q3 == null || q2 == null || q1 == null) return null;
                ttm += q3 + q2 + q1;
                hasValidData = true;
                break;
            }
            case "2Q": {
                Long q2 = safeGetValue(currentYear, "2Q", valueExtractor);
                Long q1 = safeGetValue(currentYear, "1Q", valueExtractor);
                if (q2 == null || q1 == null) return null;
                ttm += q2 + q1;
                hasValidData = true;
                break;
            }
            case "1Q": {
                Long q1 = safeGetValue(currentYear, "1Q", valueExtractor);
                if (q1 == null) return null;
                ttm += q1;
                hasValidData = true;
                break;
            }
        }

        log.debug("[{}] TTM({}) - 올해 {}까지 합산: {}", stockCode, fieldName, currentQuarter, ttm);

        // Step 2: 작년 잔여 분기 합산
        if (lastYear == null || lastYear.isEmpty()) {
            log.debug("[{}] TTM({}) - 작년 데이터 없음, 올해 데이터만 사용", stockCode, fieldName);
            return hasValidData ? ttm : null;
        }

        // 작년 4Q 단독 실적 계산: 4Q(연간) - 3Q - 2Q - 1Q
        Long lastYear4QStandalone = calculateLastYear4QStandalone(lastYear, valueExtractor, stockCode, fieldName);
        if (lastYear4QStandalone == null) return null;

        switch (currentQuarter) {
            case "3Q":
                // 올해 3Q까지 있으면 → 작년 4Q 단독만 필요
                ttm += (lastYear4QStandalone != null ? lastYear4QStandalone : 0L);
                break;
            case "2Q":
                // 올해 2Q까지 있으면 → 작년 4Q 단독 + 작년 3Q
                ttm += (lastYear4QStandalone != null ? lastYear4QStandalone : 0L);
                Long ly3 = safeGetValue(lastYear, "3Q", valueExtractor);
                if (ly3 == null) return null;
                ttm += ly3;
                break;
            case "1Q":
                // 올해 1Q만 있으면 → 작년 4Q 단독 + 작년 3Q + 작년 2Q
                ttm += (lastYear4QStandalone != null ? lastYear4QStandalone : 0L);
                Long ly3q = safeGetValue(lastYear, "3Q", valueExtractor);
                Long ly2q = safeGetValue(lastYear, "2Q", valueExtractor);
                if (ly3q == null || ly2q == null) return null;
                ttm += ly3q + ly2q;
                break;
        }

        log.debug("[{}] TTM({}) - 최종 TTM: {}", stockCode, fieldName, ttm);
        return hasValidData ? ttm : null;
    }

    /**
     * 작년 4Q 단독 실적 계산
     * = 작년 4Q(연간 누적) - 작년 3Q - 작년 2Q - 작년 1Q
     */
    private static Long calculateLastYear4QStandalone(
            Map<String, StockFinancial> lastYear,
            java.util.function.Function<StockFinancial, Long> valueExtractor,
            String stockCode,
            String fieldName
    ) {
        if (lastYear == null || !lastYear.containsKey("4Q")) {
            return null;
        }

        Long annual = safeGetValue(lastYear, "4Q", valueExtractor); // 연간 누적
        Long q1 = safeGetValue(lastYear, "1Q", valueExtractor);
        Long q2 = safeGetValue(lastYear, "2Q", valueExtractor);
        Long q3 = safeGetValue(lastYear, "3Q", valueExtractor);

        if (annual == null || q1 == null || q2 == null || q3 == null) {
            return null;
        }

        Long standalone = annual - q1 - q2 - q3;
        log.debug("[{}] 작년4Q 단독({}) = {} - {} - {} - {} = {}",
                stockCode, fieldName, annual, q1, q2, q3, standalone);
        return standalone;
    }

    /**
     * 해당 분기의 개별(Discrete) 실적 조회
     * - 1Q, 2Q, 3Q: DB 값이 이미 개별이므로 그대로 반환
     * - 4Q: 연간 - (1Q + 2Q + 3Q) 계산
     */
    private static Long getDiscreteValue(
            Map<String, StockFinancial> targetYear,
            Map<String, StockFinancial> prevYear,
            String quarter,
            java.util.function.Function<StockFinancial, Long> valueExtractor
    ) {
        if (targetYear == null || quarter == null) {
            return null;
        }

        // 1Q, 2Q, 3Q는 그대로 반환
        if (!"4Q".equals(quarter)) {
            return safeGetValue(targetYear, quarter, valueExtractor);
        }

        // 4Q인 경우: 연간 - (1Q + 2Q + 3Q)
        Long annual = safeGetValue(targetYear, "4Q", valueExtractor);
        if (annual == null) {
            return null;
        }

        Long q1 = safeGetValue(targetYear, "1Q", valueExtractor);
        Long q2 = safeGetValue(targetYear, "2Q", valueExtractor);
        Long q3 = safeGetValue(targetYear, "3Q", valueExtractor);
        if (q1 == null || q2 == null || q3 == null) return null;

        return annual - q1 - q2 - q3;
    }


    /**
     * 4Q 단독 값 계산: 연간 누적(4Q) - 1Q - 2Q - 3Q
     * 제무재표 하나의 값을 계산할 때 사용하는 메서드입니다.(년도 x, stockCode x)
     *
     */
    public static Long calculateQ4StandaloneValue(Long annual, Long q1, Long q2, Long q3) {
        if (annual == null || q1 == null || q2 == null || q3 == null) {
            return annual; // 계산 불가 시 원본 반환
        }
        return annual - q1 - q2 - q3;
    }


    // =========================================================================
    // 헬퍼 메서드
    // =========================================================================

    private static Long safeGetValue(
            Map<String, StockFinancial> yearData,
            String quarter,
            java.util.function.Function<StockFinancial, Long> valueExtractor
    ) {
        if (yearData == null) return null;
        StockFinancial sf = yearData.get(quarter);
        if (sf == null) return null;

        return valueExtractor.apply(sf);
    }

    private static String getPrevQuarter(String quarter) {
        return switch (quarter) {
            case "2Q" -> "1Q";
            case "3Q" -> "2Q";
            case "4Q" -> "3Q";
            case "1Q" -> "4Q"; // 작년 4Q
            default -> null;
        };
    }

    private static Double safeDivide(Double numerator, Double denominator) {
        if (numerator == null || denominator == null || denominator == 0.0) {
            return null;
        }
        return numerator / denominator;
    }

    private static Double safeDivide(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator == 0L) {
            return null;
        }
        return (double) numerator / denominator;
    }

    private static Double safeDivideToPercent(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator == 0L) {
            return null;
        }
        return (double) numerator / denominator * 100.0;
    }

    private static Double safeGrowthRate(Long current, Long previous) {
        if (current == null || previous == null || previous == 0L) {
            return null;
        }
        // 전기 적자인 경우 성장률 의미 없음
        if (previous < 0L) {
            return null;
        }
        return ((double) current / previous - 1.0) * 100.0;
    }

    private static Double calcDividendYield(Integer currentPrice, Integer lastDps) {
        if (currentPrice == null || currentPrice <= 0) return null;
        if (lastDps == null) return null;
        if (lastDps <= 0) return 0.0;

        double raw = (lastDps * 100.0) / currentPrice;
        return BigDecimal.valueOf(raw)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
