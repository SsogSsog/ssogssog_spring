package org.project.ssogssog.domain.member.repository;

import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {
    int countByMember(Member member);
    List<Strategy> findAllByMember(Member member);
    Optional<Strategy> findByIdAndMember(Long id, Member member);
}
