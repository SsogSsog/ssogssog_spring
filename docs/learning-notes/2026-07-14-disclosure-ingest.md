# 공시 임베딩 적재 배치 — 복습 노트

- 작성일: 2026-07-14
- 커리큘럼 위치: RAG 3주 커리큘럼 **Week 2 Day 3 "공시 → 청킹 → 임베딩 → pgvector 적재 배치"**
- 구현 파일:
  - `application/service/ai/ingest/DisclosureIngestService.java` (파이프라인)
  - `application/service/ai/ingest/dto/IngestResult.java` (결과 DTO)
  - `presentation/controller/ai/DisclosureIngestController.java` (수동 트리거)
- 목적: 외부 API(OpenDART) 공시를 임베딩해 pgvector에 **증분 적재**. 검색/답변은 Day4~5.

---

## 0. 오늘의 검증 결과 (N 기록)

`POST /ai/ingest-disclosures?corpCode=00126380` (삼성전자) 2회 호출:

| 호출 | fetched | skipped | ingested | 의미 |
|---|---|---|---|---|
| 1차 | 20 | 0 | **20** | 처음이라 20건 전부 적재 |
| 2차 | 20 | **20** | 0 | 같은 공시 → 전부 스킵 (중복 없음) |

- pgvector `vector_store` 테이블에 공시 **20 row** 확인 (`metadata->>'receiptNo'` 있는 행).
- **적재 규모 N = 20건** (포폴 재료).
- → **증분 적재가 실제로 작동.** 배치를 몇 번 눌러도 중복이 안 쌓임.

---

## 1. RAG에서 오늘 위치 — "적재(Ingestion)"이지 "검색"이 아님

RAG는 두 단계:

```
[1단계: 적재]  ← 오늘 Day3
  외부 API(OpenDART) → 텍스트를 벡터로 → pgvector에 미리 저장

[2단계: 검색+답변]  ← Day4~5
  질문 → 벡터 변환 → 유사한 공시 검색 → LLM이 근거로 답변
```

**왜 미리 쌓아두나**: 질문할 때마다 외부 API 부르고 임베딩하면 느리고 비쌈. 한 번 벡터로 만들어 DB에 넣어두고, 검색은 DB에서만 함.

---

## 2. 임베딩할 "텍스트"를 무엇으로? — 제목/메타데이터만

OpenDART `list.json`이 주는 것:
- **공시 목록**(제목·접수번호·날짜) — 짧음 → **이걸 임베딩** ✅
- **공시 본문**(원문 다운로드) — 수십 페이지 → 파싱·비용 부담, **향후 과제** ❌

**청킹 사실상 생략**: 제목이 짧아 512토큰 안 넘음 → 문서 1건 = 청크 1개.
- 면접 답변: "청킹 전략은 데이터 특성에 따라 — 짧은 메타데이터는 청킹 불필요, 긴 본문은 512토큰 스플리터(`TokenTextSplitter`)가 다음 단계."

---

## 3. `Document` — Spring AI의 표준 형식 (pgvector 자료형 아님)

`org.springframework.ai.document.Document` = 벡터 스토어에 넣을 "한 조각"을 표현하는 **자바 객체**. pgvector든 Redis든 공통으로 씀(저장소 바꿔도 이 코드는 그대로).

```
Document { id, text, metadata, embedding }
```

`vectorStore.add(docs)` 호출 시 pgvector `vector_store` 테이블에 **한 행(row)씩** 매핑:

| Document 필드 | → 테이블 컬럼 |
|---|---|
| id | id (uuid, 자동) |
| text | content (text) |
| metadata | metadata (jsonb) |
| (text를 임베딩한 결과) | embedding (vector(1536)) ← **자동 계산** |

### ★핵심: 임베딩은 우리가 안 부른다
우리는 `Document`에 **text와 metadata만** 채움. `add()`가 내부에서:
1. text를 꺼내 `EmbeddingModel`(Gemini)로 보내 `float[1536]` 벡터 생성
2. text·metadata·벡터를 한 row로 pgvector에 INSERT

→ W2 Day1에서 손으로 친 `embed()`가 실무에선 `add()` 안에서 자동으로 돎.

### text만 임베딩, 식별자는 metadata
- `text`(공시 제목) → 임베딩됨 (의미 검색 대상)
- `receiptNo`, `date` → metadata(JSON), 임베딩 안 됨 (정확 매칭·필터·출처용)
- **이유**: 의미로 검색할 건 공시 내용이지 접수번호가 아님.

---

## 4. 증분 적재 — "검색"이 아니라 "중복 방지 확인"

배치를 누를 때마다 OpenDART는 **매번 최근 3개월치를 통째로** 돌려줌 → 그냥 다 넣으면 중복.

**증분 판정**: 넣기 전에 "이 receiptNo 이미 있나?" 확인 → 없는 것만 넣음.

### ★함정: VectorStore엔 존재 확인 전용 API가 없다
`similaritySearch`밖에 없어서, **메타데이터 필터**를 걸어 대신함:

```java
FilterExpressionBuilder b = new FilterExpressionBuilder();
List<Document> found = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query(receiptNo)        // 형식상 필수, 판정엔 안 씀
                .topK(1)
                .filterExpression(b.eq(META_RECEIPT_NO, receiptNo).build())  // 이게 본체
                .build());
boolean exists = !found.isEmpty();
```

- 같은 `similaritySearch`지만 **벡터 유사도는 안 봄** — 오직 metadata 정확 매칭.
- Day4 진짜 검색과의 차이: 오늘=중복 방지(정확 매칭), Day4=답변용(의미 유사도).

### ★함정: metadata 키 불일치 (컴파일러가 안 잡음)
저장은 `receipt_no`, 조회는 `receiptNo`로 하면 **영원히 못 찾음** → 증분 무력화, 조용히 매번 전부 재적재.
- metadata는 `Map<String,Object>`라 어떤 문자열 키든 다 받음 → 오타 안 잡힘.
- **대응**: 키를 상수(`META_RECEIPT_NO`)로 뽑아 저장/조회가 물리적으로 같은 값을 쓰게 강제. (오타 시 컴파일 에러)
- 면접 포인트: "메타데이터 필터는 타입 안전성이 없어 키를 상수로 관리."

---

## 5. 기존 부품 재사용 — 조립이 본령

- 공시 조회: `StockIssuePort.searchDisclosures(corpCode, page)` **그대로 재사용** (Resilience4j 적용됨). 새로 안 짬.
- `VectorStore`: W2 Day2에 등록한 스프링 빈 → `JdbcTemplate`처럼 주입만 받아 씀. 내부에 그 JdbcTemplate + EmbeddingModel을 감싼 상위 도구.
- → 오늘 실제로 짠 건 "이 부품들을 잇는 파이프라인 + 증분 로직"뿐.

---

## 6. 트레이드오프 (면접 재료 / 한계 인식)

- **증분을 vector_store 메타데이터 조회로 판정** → 매 건 `similaritySearch` 호출(=매번 임베딩 비용). 학습 N 규모(20건)엔 OK, 실무 대량이면 별도 상태 테이블/유니크 제약이 정석.
  - "간단함 우선 → 규모 커지면 상태 테이블로 승격"이 올바른 판단.
- **본문 전문 미적재, 청킹/top-k 미튜닝** → 전부 "향후 과제"로 정직하게.

---

## 한 줄 요약 (면접용)

> "OpenDART 공시를 Spring AI `VectorStore.add`로 pgvector에 적재하는 배치. 임베딩은 `add`가 내부에서 자동 처리하고, 증분 적재는 접수번호를 **메타데이터 필터**로 판정해 중복을 막았다. 검증: 2회 호출로 20건 적재 → 전부 스킵(중복 0) 실증."
