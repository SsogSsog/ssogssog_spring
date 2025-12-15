package org.project.ssogssog.presentation.controller.stockmetric.enums;

public enum MarketCapBucket {
    SMALL_CAP,   // < 5,000억
    MID_CAP,     // 5,000억 ~ 3조
    LARGE_CAP;    // >= 3조

    public static Long minPrice(MarketCapBucket marketCapBucket) {

        if(marketCapBucket == null){
            return null;
        }

        return switch (marketCapBucket){
            case SMALL_CAP -> null;
            case MID_CAP -> 500000000000L;
            case LARGE_CAP -> 3000000000000L;
        };
    }

    public static Long maxPrice(MarketCapBucket marketCapBucket) {

        if(marketCapBucket == null){
            return null;
        }

        return switch (marketCapBucket){
            case SMALL_CAP -> 500000000000L;
            case MID_CAP -> 3000000000000L;
            case LARGE_CAP -> null;
        };
    }
}
