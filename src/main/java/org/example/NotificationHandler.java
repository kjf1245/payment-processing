package org.example;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.Map;

public class NotificationHandler implements RequestHandler<Map<String, Object>, Void> {
    
       @Override 
       public Void handleRequest(Map<String, Object> event, Context context) {
           System.out.println("Payment confirmed, " + "sending notification for: " + event.get("detail"));
           return null;
       }
}