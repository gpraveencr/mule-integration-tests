/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.lang.Class.forName;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.util.ExceptionUtils.NULL_ERROR_HANDLER;
import org.mule.functional.api.component.FlowAssert;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.MessagingExceptionHandlerToSystemAdapter;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ExceptionHandlingTestCase extends AbstractIntegrationTestCase {

  public static final String MESSAGE = "some message";

  private static MessagingExceptionHandler effectiveMessagingExceptionHandler;
  private static CountDownLatch latch;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-handling-test.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    effectiveMessagingExceptionHandler = null;
  }

  @Test
  public void testCustomProcessorInFlow() throws Exception {
    final Event muleEvent = runFlow("customProcessorInFlow");
    Message response = muleEvent.getMessage();

    assertThat(response, is(notNullValue()));
    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(ErrorHandler.class)));
    assertThat(((ErrorHandler) effectiveMessagingExceptionHandler).getExceptionListeners(), hasSize(1));
    MessagingExceptionHandlerAcceptor handler =
        ((ErrorHandler) effectiveMessagingExceptionHandler).getExceptionListeners().get(0);
    assertThat(handler, is(instanceOf(OnErrorPropagateHandler.class)));
    assertThat(handler.acceptsAll(), is(true));
  }

  @Test
  public void testAsyncInFlow() throws Exception {
    flowRunner("asyncInFlow").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outFlow4", 3000).getRight().get();
    assertNotNull(response);
    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(ErrorHandler.class)));
  }

  @Test
  public void testUntilSuccessfulInFlow() throws Exception {
    flowRunner("untilSuccessfulInFlow").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outFlow5", 3000).getRight().get();

    assertNotNull(response);
    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(ErrorHandler.class)));
  }

  @Test
  public void testCustomProcessorInScope() throws Exception {
    LinkedList<String> list = new LinkedList<>();
    list.add(MESSAGE);
    final Event muleEvent = flowRunner("customProcessorInScope").withPayload(list).run();
    Message response = muleEvent.getMessage();

    assertNotNull(response);
    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(ErrorHandler.class)));
  }

  @Test
  public void testCustomProcessorInTransactionalScope() throws Exception {
    flowRunner("customProcessorInTransactionalScope").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outTransactional1", 3000).getRight().get();

    assertNotNull(response);

    FlowAssert.verify("customProcessorInTransactionalScope");

    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(ErrorHandler.class)));
  }

  @Test
  public void testAsyncInTransactionalScope() throws Exception {
    testTransactionalScope("asyncInTransactionalScope", "test://outTransactional4", emptyMap());
  }

  @Test
  public void testUntilSuccessfulInTransactionalScope() throws Exception {
    testTransactionalScope("untilSuccessfulInTransactionalScope", "test://outTransactional5", emptyMap());
    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(ErrorHandler.class)));
  }

  @Test
  public void testCustomProcessorInExceptionStrategy() throws Exception {
    flowRunner("customProcessorInExceptionStrategy").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outStrategy1", 3000).getRight().get();

    assertNotNull(response);

    FlowAssert.verify("customProcessorInExceptionStrategy");

    assertTrue(effectiveMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
  }

  @Test
  public void testAsyncInExceptionStrategy() throws Exception {
    testExceptionStrategy("asyncInExceptionStrategy", emptyMap());
    assertTrue(effectiveMessagingExceptionHandler instanceof MessagingExceptionHandlerToSystemAdapter);
  }

  @Test
  public void testUntilSuccessfulInExceptionStrategy() throws Exception {
    testExceptionStrategy("untilSuccessfulInExceptionStrategy", emptyMap());
    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(MessagingExceptionHandlerToSystemAdapter.class)));
  }

  @Test
  public void testUntilSuccessfulInExceptionStrategyRollback() throws Exception {
    testExceptionStrategy("untilSuccessfulInExceptionStrategyRollback", emptyMap());
    assertThat(effectiveMessagingExceptionHandler, is(instanceOf(MessagingExceptionHandlerToSystemAdapter.class)));
  }

  @Test
  public void errorThrownByOperationInForeach() throws Exception {
    MessagingException messagingException =
        flowRunner("errorThrownByOperationInForeach").withPayload(asList("1", "2", "3")).runExpectingException();
    assertThat(messagingException.getCause(), instanceOf(ExpressionRuntimeException.class));
  }

  private void testTransactionalScope(String flowName, String expected, Map<String, Serializable> messageProperties)
      throws Exception {
    flowRunner(flowName).withPayload(MESSAGE).withInboundProperties(messageProperties).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request(expected, 3000).getRight().get();

    assertNotNull(response);
  }

  private void testExceptionStrategy(String flowName, Map<String, Serializable> messageProperties) throws Exception {
    latch = spy(new CountDownLatch(2));
    try {
      flowRunner(flowName).withPayload(MESSAGE).withInboundProperties(messageProperties).dispatch();
    } catch (Exception e) {
      // do nothing
    }

    assertFalse(latch.await(3, TimeUnit.SECONDS));
    verify(latch).countDown();
  }

  public static class ExecutionCountProcessor implements Processor {

    @Override
    public synchronized Event process(Event event) throws MuleException {
      latch.countDown();
      return event;
    }
  }

  public static class ExceptionHandlerVerifierProcessor
      implements Processor {

    @Override
    public synchronized Event process(Event event) throws MuleException {
      try {
        Field exceptionHandlerField = forName("org.mule.runtime.core.AbstractEventContext").getDeclaredField("exceptionHandler");
        exceptionHandlerField.setAccessible(true);
        EventContext eventContext = event.getInternalContext();
        effectiveMessagingExceptionHandler = (MessagingExceptionHandler) exceptionHandlerField.get(eventContext);
        while (eventContext.getParentContext().isPresent() && effectiveMessagingExceptionHandler == NULL_ERROR_HANDLER) {
          eventContext = eventContext.getInternalParentContext().get();
          effectiveMessagingExceptionHandler = (MessagingExceptionHandler) exceptionHandlerField.get(eventContext);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return event;
    }

  }
}
