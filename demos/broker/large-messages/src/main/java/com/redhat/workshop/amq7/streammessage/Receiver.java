/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.workshop.amq7.streammessage;


import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class Receiver {
   public static void main(String args[]) throws Exception{

      try {
         InitialContext context = new InitialContext();

         Queue queue = (Queue) context.lookup("queue/exampleQueue");

         ConnectionFactory cf = (ConnectionFactory) context.lookup("ConnectionFactory");

         try (
               Connection connection = cf.createConnection();
         ) {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            connection.start();

            MessageConsumer messageConsumer = session.createConsumer(queue);

            connection.start();

            BytesMessage messageReceived = (BytesMessage) messageConsumer.receive(120000);

            File outputFile = new File("huge_message_received.dat");

            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
               BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutputStream);

               //This will save the stream and wait until the entire message is written before continuing.
               messageReceived.setObjectProperty("JMS_AMQ_SaveStream", bufferedOutput);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
