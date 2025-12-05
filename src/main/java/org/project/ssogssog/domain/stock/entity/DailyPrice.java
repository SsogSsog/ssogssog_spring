package org.project.ssogssog.domain.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_price")
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
}