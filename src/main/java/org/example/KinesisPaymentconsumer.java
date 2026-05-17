package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class KinesisPaymentconsumer implements RequestHandler<KinesisEvent, Void> {
    private final SqsClient sqs = SqsClient.create();

    @Override
    public Void handleRequest(KinesisEvent event, Context context) {
        for(KinesisEvent.KinesisEventRecord record: event.getRecords()) {
            String payload = record.getKinesis().getData().toString();

            //Forward to your existing SQS queue
            sqs.sendMessage(SendMessageRequest.builder() 
                .queueUrl(System.getenv("PAYMENTS_QUEUE_URL"))
            .messageBody(payload)
        .build());

        context.getLogger().log("Forwarded to SQS: " + payload);
        }
        return null;
    }
}
