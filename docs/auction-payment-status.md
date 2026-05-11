# Auction Payment Status

경매 입찰 예치금과 정산 상태는 `AuctionPaymentStatus`로 관리한다.

| Status | Description |
| --- | --- |
| `RESERVED` | 입찰 금액이 지갑에서 차감되어 예치 중인 상태 |
| `SHIPPED` | 판매자가 발송 처리를 완료했고 구매자 수령확정을 기다리는 상태 |
| `REFUND_PENDING` | 더 높은 입찰로 추월되어 환불 처리가 대기 중인 상태 |
| `REFUNDED` | 추월된 입찰 예치금이 지갑으로 환불 완료된 상태 |
| `SETTLED` | 구매자 수령확정 또는 자동 수령확정으로 거래가 완료된 상태 |
| `DISPUTED` | 문제 신고로 정산이 보류된 상태 |

추월 입찰 발생 시 입찰 트랜잭션 안에서는 이전 최고 입찰자의 결제를 `REFUND_PENDING`으로만 변경한다. 실제 지갑 환불은 입찰 트랜잭션 커밋 이후 이벤트 리스너 또는 환불 스케줄러가 처리한다.

낙찰 후 판매자가 3일 안에 발송 처리를 하지 않으면 `RESERVED`에서 `REFUND_PENDING`으로 전환한다.
판매자가 발송 처리한 뒤 구매자가 7일 안에 수령확정을 하지 않으면 `SHIPPED`에서 `SETTLED`로 자동 전환한다.
