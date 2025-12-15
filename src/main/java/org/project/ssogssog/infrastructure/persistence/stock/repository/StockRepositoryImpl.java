package org.project.ssogssog.infrastructure.persistence.stock.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.project.ssogssog.domain.stock.entity.QDailyPrice;
import org.project.ssogssog.domain.stock.entity.QStock;
import org.project.ssogssog.domain.stock.repository.StockRepositoryCustom;
import org.project.ssogssog.presentation.controller.stock.dto.StockResponse;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepositoryCustom {


}
