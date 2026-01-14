package org.project.ssogssog.domain.member.repository;

import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {
    int countByMember(Member member);
}
