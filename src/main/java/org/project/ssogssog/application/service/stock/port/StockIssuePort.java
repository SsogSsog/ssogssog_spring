package org.project.ssogssog.application.service.stock.port;

import org.project.ssogssog.application.service.stock.usecase.dto.NewsDTO;

import java.util.List;

public interface StockIssuePort {

    List<NewsDTO> searchNews(String keyword);
}
