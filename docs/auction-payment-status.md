# Auction Payment Status

경매 입찰 예치금과 정산 상태는 `AuctionPaymentStatus`로 관리한다.

| Status | Description |
| --- | --- |
| `RESERVED` | 입찰 금액이 지갑에서 차감되어 예치 중인 상태 |
| `REFUND_PENDING` | 더 높은 입찰로 추월되어 환불 처리가 대기 중인 상태 |
| `REFUNDED` | 추월된 입찰 예치금이 지갑으로 환불 완료된 상태 |
| `SETTLED` | 낙찰 금액이 판매자에게 정산 완료된 상태 |
| `DISPUTED` | 문제 신고로 정산이 보류된 상태 |

추월 입찰 발생 시 입찰 트랜잭션 안에서는 이전 최고 입찰자의 결제를 `REFUND_PENDING`으로만 변경한다. 실제 지갑 환불은 입찰 트랜잭션 커밋 이후 이벤트 리스너 또는 환불 스케줄러가 처리한다.
