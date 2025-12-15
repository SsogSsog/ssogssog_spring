package org.project.ssogssog.presentation.controller.stockmetric.enums;

public enum StockPriceRange {
    BELOW_1000,            // 1,000 미만
    FROM_1000_TO_5000,     // 1,000 ~ 5,000
    FROM_5000_TO_10000,    // 5,000 ~ 10,000
    FROM_10000_TO_30000,   // 10,000 ~ 30,000
    FROM_30000_TO_100000,  // 30,000 ~ 100,000
    ABOVE_100000;           // 100,000 이상

    /**
     * 구간의 최소 가격 (하한, 포함)
     * BELOW_1000 은 하한이 없으니 null
     */
    public static Integer minPrice(StockPriceRange range) {

        if(range == null) {
            return null;
        }

        return switch (range) {
            case BELOW_1000          -> null;      // 하한 없음
            case FROM_1000_TO_5000   -> 1_000;
            case FROM_5000_TO_10000  -> 5_000;
            case FROM_10000_TO_30000 -> 10_000;
            case FROM_30000_TO_100000-> 30_000;
            case ABOVE_100000        -> 100_000;
        };
    }

    /**
     * 구간의 최대 가격 (상한, 포함)
     * ABOVE_100000 은 상한이 없으니 null
     */
    public static Integer maxPrice(StockPriceRange range) {

        if(range == null){
            return null;
        }

        return switch (range) {
            case BELOW_1000          -> 999;       // 1,000 미만
            case FROM_1000_TO_5000   -> 5_000;
            case FROM_5000_TO_10000  -> 10_000;
            case FROM_10000_TO_30000 -> 30_000;
            case FROM_30000_TO_100000-> 100_000;
            case ABOVE_100000        -> null;      // 상한 없음
        };
    }

}