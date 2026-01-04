package org.project.ssogssog.application.service.stock.port;

import org.project.ssogssog.application.service.stock.usecase.dto.DisclosureDTO;
import org.project.ssogssog.application.service.stock.usecase.dto.NewsDTO;

import java.util.List;

/**
 * 뉴스, 공시 정보를 담당하는 interface
 */
public interface StockIssuePort {

    // keyword에 대한 뉴스 정보 검색
    List<NewsDTO> searchNews(String keyword, int page);

    // 해당 주식에 대한 공시 정보 검색
    List<DisclosureDTO> searchDisclosures(String corpCode, int page);
}
