/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.junit.Assert.assertTrue;

import io.qameta.allure.Issue;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-11097: Reactor does not handle RejectedExecutionException's")
@Issue("MULE-11097")
public class ValidShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "5000");

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-one-way-config.xml";
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Test
  public void testStaticComponent() throws Exception {
    doShutDownTest("staticComponentResponse", "staticComponentFlow");
  }

  @Test
  public void testScriptComponent() throws Exception {
    doShutDownTest("scriptComponentResponse", "scriptComponentFlow");
  }

  @Test
  public void testExpressionTransformer() throws Exception {
    doShutDownTest("expressionTransformerResponse", "expressionTransformerFlow");
  }

  private void doShutDownTest(final String payload, final String flowName) throws MuleException, InterruptedException {
    final TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    final boolean[] results = new boolean[] {false};

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          flowRunner(flowName).withPayload(payload).dispatch();

          Message response = queueHandler.read("response", RECEIVE_TIMEOUT).getMessage();
          results[0] = payload.equals(getPayloadAsString(response));
        } catch (Exception e) {
          // Ignore
        }
      }
    };
    t.start();

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();

    muleContext.stop();

    t.join();

    assertTrue("Was not able to process message ", results[0]);
  }
}
