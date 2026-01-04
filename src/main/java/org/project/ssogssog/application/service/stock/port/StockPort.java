package org.project.ssogssog.application.service.stock.port;

public interface StockPort {
    String fetchSector(String stockCode);

    // OpenDart 에서 가져온 전체 XML 데이터 전달
    byte[] getCorpCodeZip();
}
