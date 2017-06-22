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

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.cluster.Transformer;
import org.apache.activemq.artemis.protocol.amqp.broker.AMQPMessage;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;

public class UpperCaseTransformer implements Transformer {

   @Override
   public Message transform(Message inMessage) {

      if (inMessage instanceof AMQPMessage) {
         AMQPMessage amqpMessage = (AMQPMessage) inMessage;
         String text = ((AmqpValue) amqpMessage.getProtonMessage().getBody()).getValue().toString();

         AmqpValue value = new AmqpValue(text.toUpperCase());
         ICoreMessage copy = amqpMessage.toCore();
         copy.setType(Message.TEXT_TYPE);
         ActiveMQBuffer buffer = copy.getBodyBuffer();

         buffer.writerIndex(0);
         buffer.clear();
         buffer.writeBytes(new AmqpValue(text.toUpperCase()).;

         return copy;
      }
      return inMessage;
   }
}
