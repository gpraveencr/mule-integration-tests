/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.functional.api.notification.FunctionalTestNotification;
import org.mule.functional.listener.FlowExecutionListener;
import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.test.AbstractIntegrationTestCase;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

public class MessageChunkingTestCase extends AbstractIntegrationTestCase {

  @Inject
  private NotificationListenerRegistry notificationRegistry;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/message-chunking-flow.xml";
  }

  @Test
  public void testMessageChunkingWithEvenSplit() throws Exception {
    doMessageChunking("0123456789", 5);
  }

  @Test
  public void testMessageChunkingWithOddSplit() throws Exception {
    doMessageChunking("01234567890", 6);
  }

  @Test
  public void testMessageChunkingWith100Splits() throws Exception {
    doMessageChunking("0123456789012345678901234567890123456789012345678901234567890123456789"
        + "01234567890123456789012345678901234567890123456789012345678901234567890123456789"
        + "01234567890123456789012345678901234567890123456789", 100);
  }

  @Test
  public void testMessageChunkingOneChunk() throws Exception {
    doMessageChunking("x", 1);
  }

  @Test
  public void testMessageChunkingObject() throws Exception {
    final AtomicInteger messagePartsCount = new AtomicInteger(0);
    final SimpleSerializableObject simpleSerializableObject = new SimpleSerializableObject("Test String", true, 99);

    // find number of chunks
    final int parts = (int) Math.ceil((SerializationUtils.serialize(simpleSerializableObject).length / (double) 2));

    // Listen to events fired by the ChunkingReceiver service
    notificationRegistry.registerListener(notification -> {
      assertEquals("ChunkingObjectReceiver", ((AbstractServerNotification) notification).getResourceIdentifier());
      // Test that we have received all chunks in the correct order
      Object reply = ((FunctionalTestNotification) notification).getMessage().getPayload().getValue();
      // Check if Object is of Correct Type
      assertTrue(reply instanceof SimpleSerializableObject);
      SimpleSerializableObject replySimpleSerializableObject = (SimpleSerializableObject) reply;
      // Check that Contents are Identical
      assertEquals(simpleSerializableObject.b, replySimpleSerializableObject.b);
      assertEquals(simpleSerializableObject.i, replySimpleSerializableObject.i);
      assertEquals(simpleSerializableObject.s, replySimpleSerializableObject.s);
    }, m -> "ChunkingObjectReceiver".equals(((AbstractServerNotification) m).getResourceIdentifier()));

    // Listen to Message Notifications on the Chunking receiver so we can
    // determine how many message parts have been received
    FlowExecutionListener flowExecutionListener =
        new FlowExecutionListener("ChunkingObjectReceiver", notificationListenerRegistry);
    flowExecutionListener.addListener(source -> messagePartsCount.getAndIncrement());

    flowRunner("ObjectReceiver").withPayload(simpleSerializableObject).run();
    // Ensure we processed expected number of message parts
    assertEquals(parts, messagePartsCount.get());
  }

  protected void doMessageChunking(final String data, int partsCount) throws Exception {
    final AtomicInteger messagePartsCount = new AtomicInteger(0);

    // Listen to events fired by the ChunkingReceiver service
    notificationListenerRegistry.registerListener(notification -> {
      assertEquals("ChunkingReceiver", ((AbstractServerNotification) notification).getResourceIdentifier());

      // Test that we have received all chunks in the correct order
      Object reply = ((FunctionalTestNotification) notification).getReplyMessage();
      assertEquals(data + " Received", reply);
    }, m -> "ChunkingReceiver".equals(((AbstractServerNotification) m).getResourceIdentifier()));

    // Listen to Message Notifications on the Chunking receiver so we can
    // determine how many message parts have been received
    FlowExecutionListener flowExecutionListener = new FlowExecutionListener("ChunkingReceiver", notificationListenerRegistry);
    flowExecutionListener.addListener(notificationInfo -> messagePartsCount.getAndIncrement());
    flowRunner("Receiver").withPayload(data).run();
    // Ensure we processed expected number of message parts
    assertEquals(partsCount, messagePartsCount.get());
  }
}
