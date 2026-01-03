package org.project.ssogssog.infrastructure.scheduler.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class ShedLock { // 중복 실행에 대한 안전장치

    @Id
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "lock_until", nullable = false)
    private LocalDateTime lockUntil;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", nullable = false)
    private String lockedBy;

    // Getter & Setter
    // 생성자도 필요시 작성
}