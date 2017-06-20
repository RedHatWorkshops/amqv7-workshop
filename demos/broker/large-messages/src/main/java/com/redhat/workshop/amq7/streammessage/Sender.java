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
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Sender {

   private static final long FILE_SIZE = 1L;// * 1024 * 1024 * 1024; // 2 GiB message

   public static void main(String args[]) {
      try {
         File inputFile = new File("huge_message_to_send.dat");

         createFile(inputFile, FILE_SIZE);

         InitialContext context = new InitialContext();

              Queue queue = (Queue) context.lookup("queue/exampleQueue");

              ConnectionFactory cf = (ConnectionFactory) context.lookup("ConnectionFactory");

              try (
                    Connection connection = cf.createConnection();
              ) {
                 Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                 MessageProducer producer = session.createProducer(queue);
                 BytesMessage bytesMessage = session.createBytesMessage();
                 FileInputStream fileInputStream = new FileInputStream(inputFile);
                 BufferedInputStream bufferedInput = new BufferedInputStream(fileInputStream);
                 bytesMessage.setObjectProperty("JMS_AMQ_InputStream", bufferedInput);
                 producer.send(bytesMessage);
              }
           } catch (Exception e) {
              e.printStackTrace();
           }
        }
   private static void createFile(final File file, final long fileSize) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(file);
        try (BufferedOutputStream buffOut = new BufferedOutputStream(fileOut)) {
           byte[] outBuffer = new byte[1024 * 1024];
           for (long i = 0; i < fileSize; i += outBuffer.length) {
              buffOut.write(outBuffer);
           }
        }
     }
}
