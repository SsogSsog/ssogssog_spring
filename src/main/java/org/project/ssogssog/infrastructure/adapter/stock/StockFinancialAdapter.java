package org.project.ssogssog.infrastructure.adapter.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockFinancialPort;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockFinancialAdapter implements StockFinancialPort {

    // Spring이 제공하는 ObjectMapper 주입 (JSON 파싱용)
    private final ObjectMapper objectMapper;

    private final OpenDartClient openDartClient;


    @Override
    public JsonNode getFinancialInfo(String corpCode, Integer year, String reportCode) {

        try {
            String response = openDartClient.getFinancialInfo(corpCode, year, reportCode);
            JsonNode root = objectMapper.readTree(response);
            return root;
        } catch (JsonProcessingException e) {
            log.warn("OpenDART JSON 파싱 실패 corpCode={}, year={}, reportCode={}", corpCode, year, reportCode, e);
            return null;
        }


    }
}
