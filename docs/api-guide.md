# API Guide

## 목적
이 문서는 본 프로젝트의 API 문서 관리 방식과 갱신 절차를 정리한 가이드이다.  
`docs/openapi.yaml` 파일은 협업을 위한 기준 문서이며, API 변경 사항이 발생한 경우 반드시 최신 상태로 유지해야 한다.

---

## 기본 원칙

- `docs/openapi.yaml` 파일은 **수동 갱신이 필수**이다.
- API 구현이 변경되면 OpenAPI 스펙도 함께 갱신한다.
- PR 또는 커밋 전, 실제 서버 기준으로 최신 스펙을 반영한다.
- 문서와 실제 구현이 다르지 않도록 유지한다.

---

## 행동 규칙

### 1. API 변경 발생 시
다음과 같은 경우 `docs/openapi.yaml` 갱신이 필요하다.

- 새로운 API를 추가한 경우
- 기존 API의 path, method, parameter가 변경된 경우
- request body 구조가 변경된 경우
- response body 구조가 변경된 경우
- 응답 상태 코드가 변경된 경우
- Swagger 어노테이션(`@Operation`, `@Schema`, `@Tag` 등) 내용이 변경된 경우

### 2. 갱신이 필요 없는 경우
다음과 같은 경우는 `docs/openapi.yaml` 갱신 대상이 아니다.

- 내부 비즈니스 로직만 변경된 경우
- Service 내부 구현만 변경된 경우
- Repository/DB 접근 로직만 변경된 경우
- 성능 개선, 리팩토링, 로그 추가만 이루어진 경우

단, 위 경우라도 Swagger에 노출되는 요청/응답 구조가 바뀌었다면 반드시 갱신해야 한다.

---

## 갱신 절차

1. Controller / Service 구현 또는 수정
2. 서버 실행
3. Swagger UI에서 API 테스트 완료
    - `/swagger-ui.html`
    - 또는 `/swagger-ui/index.html`
4. 자동 생성된 OpenAPI 스펙을 추출하여 `docs/openapi.yaml` 갱신
5. 변경 사항 확인 후 커밋

---

## 갱신 방법

아래 명령어로 최신 OpenAPI 스펙을 추출한다.

```bash
curl http://localhost:8080/api-docs.yaml > docs/openapi.yaml
커밋 메시지(예시) :  "docs: 상품/경매 API 스펙 갱신"
```
