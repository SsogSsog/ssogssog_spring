package org.project.ssogssog.domain.member.repository;

import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.StockLike;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockLikeRepository extends JpaRepository<StockLike, Long> {
    Optional<StockLike> findByMemberAndStock(Member member, Stock stock);


    @Query("SELECT sl.stock.id FROM StockLike sl WHERE sl.member.uuid = :uuid")
    List<Long> findStockIdsByMemberUuid(@Param("uuid") String uuid);

    // JPA 장점을 살리기 위해 해당 메서드에 한정해 _ 허용
    boolean existsByMember_UuidAndStock_StockCode(String uuid, String stockCode);
}
