package org.project.ssogssog.domain.stockmetric.vo;

public record YearQuarter(int year, String quarter) {

    public static YearQuarter prevQuarter(int year, String quarter) {
        return switch (quarter) {
            case "1Q" -> new YearQuarter(year - 1, "4Q");
            case "2Q" -> new YearQuarter(year, "1Q");
            case "3Q" -> new YearQuarter(year, "2Q");
            case "4Q" -> new YearQuarter(year, "3Q");
            default -> throw new IllegalArgumentException("Invalid quarter: " + quarter);
        };
    }

    public static YearQuarter prevYear(int year, String quarter) {
        return new YearQuarter(year - 1, quarter);
    }

}

