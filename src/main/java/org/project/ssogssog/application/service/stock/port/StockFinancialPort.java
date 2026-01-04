package org.project.ssogssog.application.service.stock.port;

import com.fasterxml.jackson.databind.JsonNode;

public interface StockFinancialPort {

    JsonNode getFinancialInfo(String corpCode, Integer year, String reportCode);
}
