package org.project.ssogssog.application.service.stock.port;

/**
 * 주식 기본 정보를 담당하는 interface
 */
public interface StockPort {
    // 주식에 대한 sector(분야) 전달
    String fetchSector(String stockCode);

    // OpenDart 에서 가져온 전체 corpCode XML 데이터 전달
    byte[] getCorpCodeZip();

    Integer fetchLastDps(String corpCode, String year);
}
