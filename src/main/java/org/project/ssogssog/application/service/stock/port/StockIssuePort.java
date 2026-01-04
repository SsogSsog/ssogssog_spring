package org.project.ssogssog.application.service.stock.port;

import org.project.ssogssog.application.service.stock.usecase.dto.DisclosureDTO;
import org.project.ssogssog.application.service.stock.usecase.dto.NewsDTO;

import java.util.List;

/**
 * 뉴스, 공시 정보를 담당하는 interface
 */
public interface StockIssuePort {

    List<NewsDTO> searchNews(String keyword, int page);
    List<DisclosureDTO> searchDisclosures(String corpCode, int page);
}
