/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.workshop.amq7.addressing;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;
import java.util.UUID;

public class JMSSubscriptions {

   public static void main(String args[]) throws Exception {

      try {
         InitialContext context = new InitialContext();

         Topic volatileSubscription = (Topic) context.lookup("volatileSubscription");
         Topic durableSubscription = (Topic) context.lookup("durableSubscription");
         Topic sharedVolatileSubscription = (Topic) context.lookup("sharedVolatileSubscription");
         Topic sharedDurableSubscription = (Topic) context.lookup("sharedDurableSubscription");

         ConnectionFactory cf = (ConnectionFactory) context.lookup("ConnectionFactory");

         try (Connection connection = cf.createConnection()) {
            connection.setClientID("ThisIsTheClientID");
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create volatile subscription
            MessageConsumer volatileSubcriber = session.createConsumer(volatileSubscription);
            MessageConsumer durableSubcriber = session.createDurableSubscriber(durableSubscription, "durableSubName");
            MessageConsumer volatileSharedSubcriber = session.createSharedConsumer(sharedVolatileSubscription, "sharedVolatileSubName");
            MessageConsumer durableSharedSubcriber = session.createSharedDurableConsumer(sharedDurableSubscription, "sharedDurableSubName");

            System.out.println("========================");
            System.out.println(" Created JMS Subscriptions");
            System.out.println("========================");

            while(true) {
               Message m = null;
               m = volatileSubcriber.receiveNoWait();
               if (m != null) {
                  System.out.println("Volatile Subscriber: " + m);
                  m = null;
               }

               m = durableSubcriber.receiveNoWait();
               if (m != null) {
                  System.out.println("Durable Subscriber: " + m);
                  m = null;
               }

               m = volatileSharedSubcriber.receiveNoWait();
               if (m != null) {
                  System.out.println("Shared Volatile Subscriber: " + m);
                  m = null;
               }

               m = durableSharedSubcriber.receiveNoWait();
               if (m != null) {
                  System.out.println("Shared Durable Subscriber: " + m);
                  m = null;
               }

            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
