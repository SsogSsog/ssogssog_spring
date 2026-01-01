package org.project.ssogssog.application.service.stock.port;

import org.project.ssogssog.application.service.stock.usecase.dto.DisclosureDTO;
import org.project.ssogssog.application.service.stock.usecase.dto.NewsDTO;

import java.util.List;

public interface StockIssuePort {

    List<NewsDTO> searchNews(String keyword, int page);
    List<DisclosureDTO> searchDisclosures(String corpCode, int page);
}
