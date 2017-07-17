/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.config.dsl.ParsersPluginTest;
import org.mule.tests.parsers.api.CustomProcessorChainRouter;

import javax.inject.Inject;

import org.junit.Test;

public class ProcessorChainRouterTestCase extends AbstractIntegrationTestCase implements ParsersPluginTest {

  @Inject
  private ConfigurationComponentLocator componentLocator;

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/processor-chain-router-config.xml";
  }

  @Test
  public void test() {
    CustomProcessorChainRouter chainRouter =
        (CustomProcessorChainRouter) componentLocator.find(Location.builder().globalName("chainRouter").build()).get();
    chainRouter.process(org.mule.runtime.api.event.Event.builder().message(Message.builder().nullPayload().build()).build());
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }
}
