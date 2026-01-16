package org.project.ssogssog.domain.member.repository;

import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.StockLike;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockLikeRepository extends JpaRepository<StockLike, Long> {
    Optional<StockLike> findByMemberAndStock(Member member, Stock stock);
    List<StockLike> findAllByMember(Member member);
    boolean existsByMemberAndStock(Member member, Stock stock);
}
