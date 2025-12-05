package org.project.ssogssog.domain.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;
import org.project.ssogssog.domain.stock.enums.Country;
import org.project.ssogssog.domain.stock.enums.MarketType;

@Entity
@Table(name = "stock") // 테이블 이름
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 시스템 내부 식별자

    @Column(unique = true, nullable = false)
    private String stockCode; // 주식 종목코드 (예: 005930)

    @Column(unique = true, nullable = false)
    private String corpCode;  // DART 고유번호 (예: 00126380)

    private String corpName;  // 회사명 (예: 삼성전자)

    @Enumerated(EnumType.STRING)
    private MarketType marketType;  // 주식 종류 (예: 코스피, 코스닥)

    @Enumerated(EnumType.STRING)
    private Country country;  // 나라 (예: KR)

    // 추후 KIS API 등을 통해 업데이트할 섹터 정보 (초기엔 null)
    private String sector;

    // --- [데이터 갱신용 메소드] --
    // 이미 존재하는 종목이라면 이름이나 고유번호가 바뀌었을 때 업데이트
    public void updateCorpInfo(String corpCode, String corpName) {
        this.corpCode = corpCode;
        this.corpName = corpName;
    }

    public void updateSector(String sector) {
        this.sector = sector;
    }
}
