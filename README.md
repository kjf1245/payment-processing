## Payment Processing Pipeline

```
CLIENT
  │
  └──► POST /payments (with idempotency-key)
       │
       ▼
   API Gateway (API key validated)
       │
       ▼
   Lambda: PaymentValidator
   ├─ Validates schema
   ├─ Checks idempotency key (Redis/DynamoDB)
   └─ Returns 202 Accepted immediately
       │
       ▼
   SQS Queue: cox-payments-queue
   ├─ 15-min visibility timeout
   └─ Max retries: 3
       │
       ▼
   Lambda: PaymentProcessor
   ├─ Deduplicates via TTL key
   ├─ Calls payment gateway (stripe/test)
   └─ Persists to DynamoDB
       │
       ├─ Success
       │  └──► EventBridge (PaymentProcessed event)
       │            │
       │            ▼
       │       Lambda: NotifyStakeholders
       │       ├─ SNS notification
       │       ├─ Slack webhook
       │       └─ Email confirmation
       │
       └─ Failure
          └──► SQS DLQ (Dead Letter Queue)
                    │
                    ▼
               Lambda: DLQHandler
               ├─ Logs failure reason
               ├─ Creates incident ticket
               └─ Alerts ops team
```
