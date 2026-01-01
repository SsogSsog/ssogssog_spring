package org.project.ssogssog.domain.stock.policy;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/***
 * policy 패키지: 매핑 규칙/레지스트리/도메인 정책(emoji 등)
 */
@Component
public class ThemeEmojiRegistry {

    private static final Map<String, String> THEME_TO_EMOJI;

    static {
        // LinkedHashMap: 선언 순서 유지(디버깅/관리 편함)
        Map<String, String> m = new LinkedHashMap<>();

        m.put("IT 서비스", "💻");
        m.put("건설", "🏗️");
        m.put("금속", "⛓️");
        m.put("금융", "💰");
        m.put("기계·장비", "⚙️");
        m.put("기타제조", "🏭");
        m.put("리츠", "🏢");
        m.put("보험", "🛡️");
        m.put("부동산", "🏠");
        m.put("비금속", "🧱");
        m.put("섬유·의류", "👕");
        m.put("오락·문화", "🎭");
        m.put("외국증권", "🌍");
        m.put("운송·창고", "🚚");
        m.put("운송장비·부품", "🚗");
        m.put("유통", "🛒");
        m.put("음식료·담배", "🍽️");
        m.put("의료·정밀기기", "🩺");
        m.put("인프라투용", "🛣️");
        m.put("일반서비스", "🧾");
        m.put("전기·가스", "⚡");
        m.put("전기·전자", "🔌");
        m.put("제약", "💊");
        m.put("제조", "🏭");
        m.put("종이·목재", "📄");
        m.put("증권", "📈");
        m.put("출판·매체복제", "📰");
        m.put("통신", "📡");
        m.put("화학", "🧪");

        THEME_TO_EMOJI = Collections.unmodifiableMap(m);
    }

    /** themeName -> emoji */
    public String getEmoji(String themeName) {
        return THEME_TO_EMOJI.getOrDefault(themeName, "📌");
    }

    /** 전체 매핑 조회(읽기 전용) */
    public Map<String, String> snapshot() {
        return THEME_TO_EMOJI;
    }
}
