package org.project.ssogssog.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "member") // 테이블 이름
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 시스템 내부 식별자

    @Column(unique = true, length = 36) // UUID 길이
    private String uuid;

    private String fcmToken; // 나중에 알림 보낼 때 사용

    @CreatedDate
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Strategy> strategy;

    // 생성자
    public Member(String uuid, String fcmToken) {
        this.uuid = uuid;
        this.fcmToken = fcmToken;
    }

    // 토큰 업데이트 메서드 (앱 재설치 등으로 토큰 바뀔 때 사용)
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
