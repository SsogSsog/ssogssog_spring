package org.project.ssogssog.application.service.stock.port;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 재무제표 정보를 담당하는 interface
 */
public interface StockFinancialPort {
    // 해당 주식의 특정 년도/분기의 재무제표 가져오기
    JsonNode getFinancialInfo(String corpCode, Integer year, String reportCode);
}
