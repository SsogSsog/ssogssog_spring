package org.project.ssogssog.domain.member.repository;

import org.project.ssogssog.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUuid(String uuid);

}