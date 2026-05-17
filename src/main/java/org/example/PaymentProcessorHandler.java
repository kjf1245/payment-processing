package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


public class PaymentProcessorHandler implements RequestHandler<SQSEvent, Void> {
   private final DynamoDbClient dynamoDB = DynamoDbClient.create();
   private final EventBridgeClient eventBridge = EventBridgeClient.create();

   @Override
   public Void handleRequest(SQSEvent event, Context context) {
       for (SQSEvent.SQSMessage message : event.getRecords()) {
           processPayment(message.getBody());
       }
       return null;
   }

   private void processPayment(String paymentJson) {
       //Store in DynamoDB
       dynamoDB.putItem(PutItemRequest.builder()
           .tableName("payments")
           .item(Map.of(
               "customerId", AttributeValue.fromS("customer-"+UUID.randomUUID().toString()),
               "paymentId", AttributeValue.fromS(UUID.randomUUID().toString()),
               "status", AttributeValue.fromS("PROCESSED"),
               "createdAt", AttributeValue.fromS(Instant.now().toString()),
               "payload", AttributeValue.fromS(paymentJson)
           ))
           .build());

       // Publish event - downstream services react to this
       eventBridge.putEvents(PutEventsRequest.builder()
           .entries(PutEventsRequestEntry.builder()
               .eventBusName("payments-bus")
               .source("payments.processor")
               .detailType("paymentProcessed")
               .detail(paymentJson)
               .build())
           .build());
   }
}