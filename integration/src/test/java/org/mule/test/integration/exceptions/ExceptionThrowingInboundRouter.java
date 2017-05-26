/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.filters.AcceptAllFilter;

public class ExceptionThrowingInboundRouter extends MessageFilter {

  public ExceptionThrowingInboundRouter() {
    super(new AcceptAllFilter());
  }

  @Override
  public boolean accept(Event event, Event.Builder builder) {
    throw new RuntimeException();
  }
}
