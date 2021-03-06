/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;

public class HttpTransformTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort2 = new DynamicPort("port2");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/sync/http-transform-flow.xml";
  }

  @Test
  public void testBinary() throws Exception {
    ArrayList<Integer> payload = new ArrayList<>();
    payload.add(42);
    HttpRequest httpRequest = HttpRequest.builder().uri(format("http://localhost:%d/RemoteService", httpPort2.getNumber()))
        .entity(new ByteArrayHttpEntity(muleContext.getObjectSerializer().getExternalProtocol().serialize(payload)))
        .method(POST).build();
    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    Object result = muleContext.getObjectSerializer().getExternalProtocol().deserialize(httpResponse.getEntity().getContent());
    assertThat(result, is(payload));
  }

  @Test
  public void testBinaryWithBridge() throws Exception {
    Object payload = Arrays.asList(42);
    Message message = flowRunner("LocalService").withPayload(payload).run().getMessage();
    assertNotNull(message);
    assertThat(message.getPayload().getValue(), equalTo(payload));
  }
}
