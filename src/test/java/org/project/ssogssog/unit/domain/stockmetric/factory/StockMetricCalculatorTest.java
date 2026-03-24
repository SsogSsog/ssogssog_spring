package org.project.ssogssog.unit.domain.stockmetric.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stockmetric.factory.StockMetricCalculator;
import org.project.ssogssog.domain.stockmetric.vo.MetricValues;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockMetricCalculator 단위 테스트
 *
 * 테스트 범위:
 * 1. 필수 데이터 검증 (Validation)
 * 2. 분기별 TTM 계산 (Core Logic)
 * 3. TTM 계산 실패 케이스
 * 4. 개별 지표 계산 (PER, PBR, ROE 등)
 * 5. 성장률 계산 (QoQ, YoY)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockMetricCalculator 테스트")
class StockMetricCalculatorTest {

    // 필수 데이터 검증
    @Nested
    @DisplayName("필수 데이터 검증")
    class ValidationTests {

        @Test
        @DisplayName("DailyPrice가 null이면 null 반환")
        void returns_null_when_dailyPrice_is_null() {
            // Given
            Stock stock = createStock("005930");
            Map<String, StockFinancial> currentYear = createFullYearData();

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    stock, null, currentYear, null, "4Q"
            );

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("currentYear가 null이면 null 반환")
        void returns_null_when_currentYear_is_null() {
            // Given
            Stock stock = createStock("005930");
            DailyPrice dailyPrice = createDailyPrice(70000, 400000000000L, 5969783000L);

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    stock, dailyPrice, null, null, "4Q"
            );

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("currentYear가 empty이면 null 반환")
        void returns_null_when_currentYear_is_empty() {
            // Given
            Stock stock = createStock("005930");
            DailyPrice dailyPrice = createDailyPrice(70000, 400000000000L, 5969783000L);
            Map<String, StockFinancial> emptyMap = new HashMap<>();

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    stock, dailyPrice, emptyMap, null, "4Q"
            );

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("currentQuarter 데이터가 없으면 null 반환")
        void returns_null_when_currentQuarter_data_missing() {
            // Given - 3Q 요청하는데 currentYear에 3Q가 없음
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L)
                    // 3Q 없음!
            );

            // When - 3Q 기준으로 계산 요청
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear,
                    null,
                    "3Q"  // 존재하지 않는 분기
            );

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Stock이 null이면 null 반환")
        void returns_null_when_stock_is_null() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L),
                    "3Q", createFinancial(11000L, 2300L, 5500L, 52000L, 26000L),
                    "4Q", createFinancial(50000L, 10000L, 25000L, 53000L, 26500L)
            );

            // When - Stock null로 전달
            MetricValues result = StockMetricCalculator.calculate(
                    null,  // Stock null
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear,
                    null,
                    "4Q"
            );

            // Then - Stock null이면 계산 불가
            assertNull(result);
        }
    }

    // 분기별 TTM 계산 (핵심!)
    @Nested
    @DisplayName("분기별 TTM 계산")
    class TTMCalculationTests {

        @Test
        @DisplayName("4Q 기준: 연간 누적값을 그대로 사용")
        void calculate_4Q_uses_annual_cumulative() {
            // Given - 올해 전체 분기 데이터
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L),
                    "3Q", createFinancial(11000L, 2300L, 5500L, 52000L, 26000L),
                    "4Q", createFinancial(50000L, 10000L, 25000L, 53000L, 26500L)  // 연간 누적
            );

            Stock stock = createStock("005930");
            DailyPrice dailyPrice = createDailyPrice(70000, 400000000000L, 5969783000L);

            // When - 4Q 기준으로 계산
            MetricValues result = StockMetricCalculator.calculate(
                    stock, dailyPrice, currentYear, null, "4Q"
            );

            // Then
            assertNotNull(result);

            // TTM 순이익 = 4Q 그대로 = 10000L
            // EPS = 10000 / 5969783000 ≈ 0.00000168
            // PER = 70000 / 0.00000168 ≈ 41,666,666
            assertNotNull(result.per());
            assertTrue(result.per() > 0, "PER은 양수여야 함");

            // ROE = (10000 / 53000) * 100 ≈ 18.87%
            assertNotNull(result.roe());
            assertEquals(18.87, result.roe(), 0.5);

            // 순이익률 = (10000 / 50000) * 100 = 20%
            assertEquals(20.0, result.netProfitMargin(), 0.01);
        }

        @Test
        @DisplayName("3Q 기준: 올해 1Q+2Q+3Q + 작년 4Q단독")
        void calculate_3Q_with_ttm_logic() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L),
                    "3Q", createFinancial(11000L, 2300L, 5500L, 52000L, 26000L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "1Q", createFinancial(9000L, 1800L, 4500L, 48000L, 24000L),
                    "2Q", createFinancial(9500L, 1900L, 4700L, 49000L, 24500L),
                    "3Q", createFinancial(9800L, 2000L, 4900L, 49500L, 24750L),
                    "4Q", createFinancial(40000L, 8000L, 20000L, 50000L, 25000L)  // 연간 누적
            );

            Stock stock = createStock("005930");
            DailyPrice dailyPrice = createDailyPrice(70000, 400000000000L, 5969783000L);

            // When - 3Q 기준으로 계산
            MetricValues result = StockMetricCalculator.calculate(
                    stock, dailyPrice, currentYear, lastYear, "3Q"
            );

            // Then
            assertNotNull(result);

            // TTM 순이익 계산:
            // 올해: 2000 + 2500 + 2300 = 6800
            // 작년 4Q 단독: 8000 - 1800 - 1900 - 2000 = 2300
            // TTM = 6800 + 2300 = 9100

            // TTM 매출 계산:
            // 올해: 10000 + 12000 + 11000 = 33000
            // 작년 4Q 단독: 40000 - 9000 - 9500 - 9800 = 11700
            // TTM = 33000 + 11700 = 44700

            // ROE = (9100 / 52000) * 100 ≈ 17.5%
            assertNotNull(result.roe());
            assertEquals(17.5, result.roe(), 0.5);

            // 순이익률 = (9100 / 44700) * 100 ≈ 20.36%
            assertNotNull(result.netProfitMargin());
            assertEquals(20.36, result.netProfitMargin(), 0.5);
        }

        @Test
        @DisplayName("2Q 기준: 올해 1Q+2Q + 작년 3Q+4Q단독")
        void calculate_2Q_with_ttm_logic() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "1Q", createFinancial(9000L, 1800L, 4500L, 48000L, 24000L),
                    "2Q", createFinancial(9500L, 1900L, 4700L, 49000L, 24500L),
                    "3Q", createFinancial(9800L, 2000L, 4900L, 49500L, 24750L),
                    "4Q", createFinancial(40000L, 8000L, 20000L, 50000L, 25000L)
            );

            // When - 2Q 기준으로 계산
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "2Q"
            );

            // Then
            // TTM 순이익 = (2000 + 2500) + 2000 + (8000 - 1800 - 1900 - 2000)
            //            = 4500 + 2000 + 2300 = 8800
            assertNotNull(result);
            assertNotNull(result.per());
            assertNotNull(result.roe());
        }

        @Test
        @DisplayName("1Q 기준: 올해 1Q + 작년 2Q+3Q+4Q단독")
        void calculate_1Q_with_ttm_logic() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "1Q", createFinancial(9000L, 1800L, 4500L, 48000L, 24000L),
                    "2Q", createFinancial(9500L, 1900L, 4700L, 49000L, 24500L),
                    "3Q", createFinancial(9800L, 2000L, 4900L, 49500L, 24750L),
                    "4Q", createFinancial(40000L, 8000L, 20000L, 50000L, 25000L)
            );

            // When - 1Q 기준으로 계산
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "1Q"
            );

            // Then
            // TTM 순이익 = 2000 + 1900 + 2000 + (8000 - 1800 - 1900 - 2000)
            //            = 2000 + 1900 + 2000 + 2300 = 8200
            assertNotNull(result);
            assertNotNull(result.roe());
        }
    }

    // ==========================================
    // 3. TTM 계산 실패 케이스
    // ==========================================
    @Nested
    @DisplayName("3. TTM 계산 실패 케이스")
    class TTMFailureTests {

        @Test
        @DisplayName("1Q인데 작년 데이터 없으면 올해 데이터만 사용")
        void calculate_1Q_without_lastYear_uses_current_only() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L)
            );

            // When - 작년 데이터 없이 계산
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear,
                    null,  // 작년 데이터 없음
                    "1Q"
            );

            // Then - 올해 1Q 데이터만으로 계산
            // TTM = 올해 1Q만 = 2000
            assertNotNull(result);
            assertNotNull(result.per());
        }

        @Test
        @DisplayName("3Q 기준인데 작년 4Q 단독 계산 실패 - TTM null이지만 다른 지표는 계산")
        void ttm_fails_but_other_metrics_calculated_when_4Q_standalone_fails() {
            // Given - 올해 1~3Q 모두 있음
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L),
                    "3Q", createFinancial(11000L, 2300L, 5500L, 52000L, 26000L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "4Q", createFinancial(40000L, 8000L, 20000L, 50000L, 25000L)
                    // 1Q, 2Q, 3Q 없음 → 4Q 단독 계산 불가!
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "3Q"
            );

            // Then
            // calculateLastYear4QStandalone가 null 반환하므로 TTM 계산 실패
            // 하지만 전체 결과는 null이 아님
            assertNotNull(result);
            assertNull(result.per());  // TTM 필요
            assertNull(result.roe());  // TTM 필요

            // TTM 무관 지표는 계산됨
            assertNotNull(result.pbr());
            assertNotNull(result.salesGrowthQoQ());  // QoQ는 올해 데이터만으로 계산
        }

        @Test
        @DisplayName("2Q 기준인데 올해 1Q 데이터 누락 - TTM 계산 실패하지만 일부 지표는 계산")
        void ttm_fails_but_other_metrics_calculated_when_1Q_missing() {
            // Given - 2Q는 있는데 1Q가 없음
            Map<String, StockFinancial> currentYear = Map.of(
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L)
                    // 1Q 없음!
            );

            Map<String, StockFinancial> lastYear = createFullYearData();

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "2Q"
            );

            // Then
            // calculateTTM에서 2Q 계산 시 1Q가 null이면 TTM은 null
            // 하지만 전체 결과는 null이 아니고, TTM 관련 지표만 null
            assertNotNull(result);
            assertNull(result.per());  // TTM 순이익 필요
            assertNull(result.roe());  // TTM 순이익 필요
            assertNull(result.netProfitMargin());  // TTM 필요

            // TTM과 무관한 지표는 계산됨
            assertNotNull(result.pbr());  // 시가총액 / 자본총계
            assertNotNull(result.debtRatio());  // 부채비율
        }

        @Test
        @DisplayName("3Q 기준인데 작년 4Q가 null - TTM null이지만 다른 지표는 계산")
        void ttm_fails_but_other_metrics_calculated_when_4Q_is_null() {
            // Given - 올해 1~3Q 모두 있음
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L),
                    "3Q", createFinancial(11000L, 2300L, 5500L, 52000L, 26000L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "1Q", createFinancial(9000L, 1800L, 4500L, 48000L, 24000L),
                    "2Q", createFinancial(9500L, 1900L, 4700L, 49000L, 24500L),
                    "3Q", createFinancial(9800L, 2000L, 4900L, 49500L, 24750L)
                    // 4Q 없음!
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "3Q"
            );

            // Then
            // calculateLastYear4QStandalone에서 4Q가 없으면 null 반환
            // 전체 결과는 null이 아님
            assertNotNull(result);
            assertNull(result.per());  // TTM 필요
            assertNull(result.roe());  // TTM 필요

            // TTM 무관 지표는 계산됨
            assertNotNull(result.pbr());
            assertNotNull(result.salesGrowthQoQ());  // 올해 3Q vs 2Q
            assertNotNull(result.salesGrowthYoY());  // 올해 3Q vs 작년 3Q
        }
    }

    @Nested
    @DisplayName("개별 지표 계산 (PER, PBR, ROE 등)")
    class MetricCalculationTests {

        @Test
        @DisplayName("PER 정상 계산")
        void calculates_per_correctly() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 50000L, 100000L, 50000L)
            );

            DailyPrice dailyPrice = createDailyPrice(
                    50000,           // currentPrice
                    300000000000L,   // marketCap
                    6000000000L      // listedShares
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"), dailyPrice, currentYear, null, "4Q"
            );

            // Then
            assertNotNull(result);

            // EPS = 20000 / 6000000000 ≈ 0.00000333...
            // PER = 50000 / 0.00000333... ≈ 15,000,000
            assertNotNull(result.per());
            assertTrue(result.per() > 0, "PER은 양수여야 함");
        }

        @Test
        @DisplayName("PER null - currentPrice가 0")
        void per_is_null_when_price_is_zero() {
            // Given
            DailyPrice dailyPrice = createDailyPrice(
                    0,  // price = 0
                    400000000000L,
                    5969783000L
            );

            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 50000L, 50000L, 25000L)
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"), dailyPrice, currentYear, null, "4Q"
            );

            // Then
            assertNotNull(result);
            assertNull(result.per());  // price = 0이므로 PER 계산 불가
        }

        @Test
        @DisplayName("PER null - ttmNetIncome이 0")
        void per_is_null_when_netIncome_is_zero() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 0L, 50000L, 50000L, 25000L)  // 순이익 0
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "4Q"
            );

            // Then
            assertNotNull(result);
            assertNull(result.per());  // 순이익 0이므로 PER 계산 불가
        }

        @Test
        @DisplayName("PER null - listedShares가 0")
        void per_is_null_when_shares_is_zero() {
            // Given
            DailyPrice dailyPrice = createDailyPrice(
                    70000,
                    400000000000L,
                    0L  // 상장주식수 0
            );

            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 50000L, 50000L, 25000L)
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"), dailyPrice, currentYear, null, "4Q"
            );

            // Then
            assertNotNull(result);
            assertNull(result.per());
        }

        @Test
        @DisplayName("PBR 정상 계산")
        void calculates_pbr_correctly() {
            // Given
            DailyPrice dailyPrice = createDailyPrice(
                    70000,
                    400000000000L,  // marketCap
                    5969783000L
            );

            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 50000L, 200000000000L, 100000000000L)
                    // totalEquity = 200000000000
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"), dailyPrice, currentYear, null, "4Q"
            );

            // Then
            // PBR = 400000000000 / 200000000000 = 2.0
            assertNotNull(result.pbr());
            assertEquals(2.0, result.pbr(), 0.01);
        }

        @Test
        @DisplayName("PBR null - totalEquity가 0")
        void pbr_is_null_when_equity_is_zero() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 50000L, 0L, 50000L)  // equity = 0
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "4Q"
            );

            // Then
            assertNotNull(result);
            assertNull(result.pbr());  // equity = 0이므로 PBR 불가
            assertNull(result.roe());  // ROE도 불가
        }

        @Test
        @DisplayName("ROE 정상 계산")
        void calculates_roe_correctly() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 50000L, 100000L, 50000L)
                    // netIncome = 20000, totalEquity = 100000
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "4Q"
            );

            // Then
            // ROE = (20000 / 100000) * 100 = 20%
            assertNotNull(result.roe());
            assertEquals(20.0, result.roe(), 0.01);
        }

        @Test
        @DisplayName("순이익률 정상 계산")
        void calculates_net_profit_margin_correctly() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 50000L, 100000L, 50000L)
                    // revenue = 100000, netIncome = 20000
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "4Q"
            );

            // Then
            // 순이익률 = (20000 / 100000) * 100 = 20%
            assertNotNull(result.netProfitMargin());
            assertEquals(20.0, result.netProfitMargin(), 0.01);
        }

        @Test
        @DisplayName("영업이익률, 부채비율 정상 계산")
        void calculates_operating_margin_and_debt_ratio() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "4Q", createFinancial(100000L, 20000L, 30000L, 100000L, 50000L)
                    // revenue = 100000, operatingProfit = 30000
                    // totalEquity = 100000, totalLiabilities = 50000
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "4Q"
            );

            // Then
            // 영업이익률 = (30000 / 100000) * 100 = 30%
            assertNotNull(result.operatingProfitMargin());
            assertEquals(30.0, result.operatingProfitMargin(), 0.01);

            // 부채비율 = (50000 / 100000) * 100 = 50%
            assertNotNull(result.debtRatio());
            assertEquals(50.0, result.debtRatio(), 0.01);
        }
    }

    // 성장률 계산 (QoQ, YoY)
    @Nested
    @DisplayName("성장률 계산 (QoQ, YoY)")
    class GrowthRateTests {

        @Test
        @DisplayName("QoQ 정상 계산 - 2Q vs 1Q")
        void calculates_qoq_growth_2Q_vs_1Q() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),  // 매출 10000, 순이익 2000
                    "2Q", createFinancial(15000L, 3000L, 7500L, 51000L, 25500L)   // 매출 15000, 순이익 3000
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "2Q"
            );

            // Then
            // 매출 QoQ = (15000 / 10000 - 1) * 100 = 50%
            assertNotNull(result.salesGrowthQoQ());
            assertEquals(50.0, result.salesGrowthQoQ(), 0.01);

            // 순이익 QoQ = (3000 / 2000 - 1) * 100 = 50%
            assertNotNull(result.netProfitGrowthQoQ());
            assertEquals(50.0, result.netProfitGrowthQoQ(), 0.01);
        }

        @Test
        @DisplayName("QoQ - 1Q는 작년 4Q 단독값과 비교")
        void calculates_qoq_growth_1Q_vs_lastYear_4Q_standalone() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(12000L, 2500L, 6000L, 50000L, 25000L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "1Q", createFinancial(9000L, 1800L, 4500L, 48000L, 24000L),
                    "2Q", createFinancial(9500L, 1900L, 4700L, 49000L, 24500L),
                    "3Q", createFinancial(9800L, 2000L, 4900L, 49500L, 24750L),
                    "4Q", createFinancial(40000L, 8000L, 20000L, 50000L, 25000L)  // 연간
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "1Q"
            );

            // Then
            // 작년 4Q 단독 매출 = 40000 - 9000 - 9500 - 9800 = 11700
            // QoQ = (12000 / 11700 - 1) * 100 ≈ 2.56%
            assertNotNull(result.salesGrowthQoQ());
            assertEquals(2.56, result.salesGrowthQoQ(), 0.1);
        }

        @Test
        @DisplayName("QoQ - 4Q는 4Q 단독값 vs 3Q 비교")
        void calculates_qoq_growth_4Q_standalone_vs_3Q() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L),
                    "3Q", createFinancial(11000L, 2300L, 5500L, 52000L, 26000L),
                    "4Q", createFinancial(50000L, 10000L, 25000L, 53000L, 26500L)  // 연간
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "4Q"
            );

            // Then
            // 4Q 단독 매출 = 50000 - 10000 - 12000 - 11000 = 17000
            // 3Q = 11000
            // QoQ = (17000 / 11000 - 1) * 100 ≈ 54.5%
            assertNotNull(result.salesGrowthQoQ());
            assertEquals(54.5, result.salesGrowthQoQ(), 0.5);
        }

        @Test
        @DisplayName("YoY 정상 계산 - 올해 2Q vs 작년 2Q")
        void calculates_yoy_growth() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                    "2Q", createFinancial(15000L, 3000L, 7500L, 51000L, 25500L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "2Q", createFinancial(12000L, 2400L, 6000L, 49000L, 24500L)
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "2Q"
            );

            // Then
            // 매출 YoY = (15000 / 12000 - 1) * 100 = 25%
            assertNotNull(result.salesGrowthYoY());
            assertEquals(25.0, result.salesGrowthYoY(), 0.01);

            // 순이익 YoY = (3000 / 2400 - 1) * 100 = 25%
            assertNotNull(result.netProfitGrowthYoY());
            assertEquals(25.0, result.netProfitGrowthYoY(), 0.01);
        }

        @Test
        @DisplayName("성장률 null - 이전 값이 0")
        void growth_rate_null_when_previous_is_zero() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(0L, 0L, 0L, 50000L, 25000L),     // 매출 0
                    "2Q", createFinancial(15000L, 3000L, 7500L, 51000L, 25500L)
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "2Q"
            );

            // Then
            // 1Q 매출이 0이므로 성장률 계산 불가
            assertNull(result.salesGrowthQoQ());
        }

        @Test
        @DisplayName("성장률 null - 이전 값이 음수 (적자)")
        void growth_rate_null_when_previous_is_negative() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "1Q", createFinancial(10000L, -1000L, -500L, 50000L, 25000L),  // 적자
                    "2Q", createFinancial(12000L, 2000L, 4000L, 51000L, 25500L)    // 흑자 전환
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, null, "2Q"
            );

            // Then
            // 1Q가 적자(-1000)이므로 순이익 성장률 의미 없음
            assertNull(result.netProfitGrowthQoQ());
        }

        @Test
        @DisplayName("YoY null - 작년 동일 분기 데이터 없음")
        void yoy_null_when_lastYear_same_quarter_missing() {
            // Given
            Map<String, StockFinancial> currentYear = Map.of(
                    "2Q", createFinancial(15000L, 3000L, 7500L, 51000L, 25500L)
            );

            Map<String, StockFinancial> lastYear = Map.of(
                    "1Q", createFinancial(10000L, 2000L, 5000L, 49000L, 24500L)
                    // 2Q 없음!
            );

            // When
            MetricValues result = StockMetricCalculator.calculate(
                    createStock("005930"),
                    createDailyPrice(70000, 400000000000L, 5969783000L),
                    currentYear, lastYear, "2Q"
            );

            // Then
            // 작년 2Q 데이터가 없어서 YoY 계산 불가
            assertNull(result.salesGrowthYoY());
            assertNull(result.netProfitGrowthYoY());
        }
    }

    // 유틸 메서드 테스트
    @Nested
    @DisplayName("calculateQ4StandaloneValue 유틸 메서드")
    class UtilityMethodTests {

        @Test
        @DisplayName("4Q 단독값 정상 계산")
        void calculates_q4_standalone_correctly() {
            // When
            Long result = StockMetricCalculator.calculateQ4StandaloneValue(
                    100000L,  // annual (4Q 연간)
                    20000L,   // 1Q
                    25000L,   // 2Q
                    30000L    // 3Q
            );

            // Then
            // 4Q 단독 = 100000 - 20000 - 25000 - 30000 = 25000
            assertEquals(25000L, result);
        }

        @Test
        @DisplayName("4Q 단독값 null - annual이 null")
        void returns_null_when_annual_is_null() {
            // When
            Long result = StockMetricCalculator.calculateQ4StandaloneValue(
                    null, 20000L, 25000L, 30000L
            );

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("4Q 단독값 null - 분기 값 중 하나라도 null")
        void returns_null_when_any_quarter_is_null() {
            // When
            Long result = StockMetricCalculator.calculateQ4StandaloneValue(
                    100000L, null, 25000L, 30000L
            );

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("4Q 단독값 음수 가능 (특수한 경우)")
        void allows_negative_q4_standalone() {
            // When - 4Q에 큰 손실이 발생한 경우
            Long result = StockMetricCalculator.calculateQ4StandaloneValue(
                    50000L,   // 연간
                    30000L,   // 1Q
                    25000L,   // 2Q
                    20000L    // 3Q
            );

            // Then
            // 4Q 단독 = 50000 - 30000 - 25000 - 20000 = -25000
            assertEquals(-25000L, result);
        }
    }

    // ==========================================
    // 헬퍼 메서드들
    // ==========================================

    /**
     * Stock 엔티티 생성 헬퍼
     */
    private Stock createStock(String stockCode) {
        return Stock.builder()
                .stockCode(stockCode)
                .lastDps(1000)  // 기본 배당금 1000원
                .build();
    }

    /**
     * DailyPrice 엔티티 생성 헬퍼
     */
    private DailyPrice createDailyPrice(Integer closePrice, Long marketCap, Long listedShares) {
        return DailyPrice.builder()
                .closePrice(closePrice)
                .marketCap(marketCap)
                .listedShares(listedShares)
                .foreignHeldShares(listedShares != null ? listedShares / 10 : null)  // 기본 10%
                .build();
    }

    /**
     * StockFinancial 엔티티 생성 헬퍼
     *
     * @param revenue          매출액
     * @param netIncome        순이익
     * @param operatingProfit  영업이익
     * @param totalEquity      자본총계
     * @param totalLiabilities 부채총계
     */
    private StockFinancial createFinancial(
            Long revenue,
            Long netIncome,
            Long operatingProfit,
            Long totalEquity,
            Long totalLiabilities
    ) {
        return StockFinancial.builder()
                .revenue(revenue)
                .netIncome(netIncome)
                .operatingProfit(operatingProfit)
                .totalEquity(totalEquity)
                .totalLiabilities(totalLiabilities)
                .build();
    }

    /**
     * 올해 전체 분기 데이터 생성 헬퍼 (테스트용 기본값)
     */
    private Map<String, StockFinancial> createFullYearData() {
        return Map.of(
                "1Q", createFinancial(10000L, 2000L, 5000L, 50000L, 25000L),
                "2Q", createFinancial(12000L, 2500L, 6000L, 51000L, 25500L),
                "3Q", createFinancial(11000L, 2300L, 5500L, 52000L, 26000L),
                "4Q", createFinancial(50000L, 10000L, 25000L, 53000L, 26500L)
        );
    }
}
