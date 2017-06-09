/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.apache.http.client.fluent.Request.Get;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.STREAMING;

import org.mule.tck.junit4.FlakinessDetectorTestRunner;
import org.mule.test.runner.RunnerDelegateTo;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_EXTENSION)
@Stories({ERROR_HANDLING, STREAMING})
@RunnerDelegateTo(FlakinessDetectorTestRunner.class)
public class HttpListenerResponseStreamingErrorHandlingTestCase extends AbstractHttpListenerErrorHandlingTestCase {

  final int socketTimeout = DEFAULT_TIMEOUT * 2;

  @Override
  protected String getConfigFile() {
    return "http-listener-response-streaming-exception-strategy-config.xml";
  }

  @Test
  public void exceptionHandledWhenBuildingResponse() throws Exception {
    final Response response =
        Get(getUrl("exceptionBuildingResponse")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(socketTimeout).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyFailed(httpResponse, "java.io.IOException: Some exception");
  }

  @Test
  public void exceptionHandledWhenSendingResponse() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponse")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(socketTimeout).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }

  @Test
  public void exceptionHandledWhenBuildingResponseFailAgain() throws Exception {
    final Response response = Get(getUrl("exceptionBuildingResponseFailAgain")).connectTimeout(DEFAULT_TIMEOUT)
        .socketTimeout(socketTimeout).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyFailed(httpResponse, "java.io.IOException: Some exception");
  }

  @Test
  public void exceptionNotHandledWhenSendingResponseFailAgain() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponseFailAgain")).connectTimeout(DEFAULT_TIMEOUT).socketTimeout(socketTimeout).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }


}
