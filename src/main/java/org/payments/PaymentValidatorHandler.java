package org.payments;

// 1. Lambda Core & Events (Required for RequestHandler and API Gateway events)
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

// import software.amazon.awssdk.core.SdkBytes;
// 2. AWS SDK v2 DynamoDB
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

// 3. AWS SDK v2 SQS
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

// import software.amazon.awssdk.services.kinesis.KinesisClient;
// import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

// 4. Standard Java Utilities
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentValidatorHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        private static final Logger log = LoggerFactory.getLogger(PaymentValidatorHandler.class);  

        private final DynamoDbClient dynamoDB = DynamoDbClient.create();
        private final SqsClient sqs = SqsClient.create();
        // private final KinesisClient kinesis = KinesisClient.create();

        @Override
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {


            log.info("Processing body: {}", event.getHeaders());
            log.info("Processing body: {}", event.getBody());

            // Parse payment from request body
            String body = event.getBody();
            String idempotencyKey = event.getHeaders().get("idempotency-key");

            // Check for duplicate
            if(isDuplicate(idempotencyKey)) {
                return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"message\": \"Payment already processed\"}");

            }

            // Record idempotency key atomically
            recordIdempotencyKey(idempotencyKey);

            // publishToKinesis(body, paymentId)

            // return response("Payment accepted", 202);   

            // Put payment on SQS for async processing
            sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(System.getenv("PAYMENTS_QUEUE_URL"))
                .messageBody(body)
                .messageGroupId("payments")
                .build());

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(202)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody("{\"message\": \"Payment accepted\"}");
        
        }
        
        private boolean isDuplicate(String idempotencyKey) {
            try {
                GetItemResponse response = dynamoDB.getItem(
                    GetItemRequest.builder()
                        .tableName("idempotency")
                        .key(Map.of("idempotencyKey",
                            AttributeValue.fromS(idempotencyKey)))
                        .build());
                    return response.hasItem();
            } catch (Exception e) {
                return false;
            }
        }

        private void recordIdempotencyKey(String idempotencyKey) {
            long ttl = Instant.now()
                .plus(24, ChronoUnit.HOURS)
                .getEpochSecond();

            dynamoDB.putItem(PutItemRequest.builder()
                .tableName("idempotency")
                .item(Map.of(
                    "idempotencyKey", AttributeValue.fromS(idempotencyKey),
                    "expiresAt", AttributeValue.fromN(String.valueOf(ttl))
                ))
                .build());
        }

        // private void publishToKinesis(String paymentJson, String paymentId) {
        //     kinesis.putRecord(PutRecordRequest.builder()
        //     .streamName("payments-stream")
        //     .partitionKey(paymentId) // routes to specific shard
        //     .data(SdkBytes.fromUtf8String(paymentJson))
        //     .build());
        // }
}