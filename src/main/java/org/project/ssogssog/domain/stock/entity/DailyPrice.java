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
}