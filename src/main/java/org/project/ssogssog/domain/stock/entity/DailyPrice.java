package org.project.ssogssog.domain.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_price",
    uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_daily_price_stock_date",
                columnNames = {"stock_id", "date"}
        )
    }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate date;

    private Integer closePrice; // 종가
    private Integer openPrice;  // 시가
    private Integer highPrice;  // 고가
    private Integer lowPrice;   // 저가

    private Long volume;        // 거래량
    private Long marketCap;     // 시가총액

    private Integer changePrice;   // 등락금액
    private Double changeRate;     // 등락률
    private Integer prevClosePrice;     // 전일 종가

    private Long listedShares;        // (상장주식수)
    private Long foreignHeldShares;   // (외국인 보유주식수)

    private Integer w52HighPrice;     // 52주 최고가
    private Integer w52LowPrice;      // 52주 최저가
    private Double pbr;               // PBR (주가순자산비율)

    /**
     * 비즈니스 로직
     */
    public void updateFrom(DailyPrice dailyPrice) {
        this.closePrice = dailyPrice.closePrice;
        this.openPrice = dailyPrice.openPrice;
        this.highPrice = dailyPrice.highPrice;
        this.lowPrice = dailyPrice.lowPrice;

        this.volume = dailyPrice.volume;
        this.marketCap = dailyPrice.marketCap;

        this.changePrice = dailyPrice.changePrice;
        this.changeRate = dailyPrice.changeRate;
        this.prevClosePrice = dailyPrice.prevClosePrice;

        this.listedShares = dailyPrice.listedShares;
        this.foreignHeldShares = dailyPrice.foreignHeldShares;

        this.w52HighPrice = dailyPrice.w52HighPrice;
        this.w52LowPrice = dailyPrice.w52LowPrice;
        this.pbr = dailyPrice.pbr;

    }
}