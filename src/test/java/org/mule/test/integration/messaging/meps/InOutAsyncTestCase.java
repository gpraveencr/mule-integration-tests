/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

// START SNIPPET: full-class
public class InOutAsyncTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Out-Async.xml";
    }

    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        //Almost any endpoint can be used here
        props.put(MuleProperties.MULE_REPLY_TO_PROPERTY, "jms://client-reply");

        MuleMessage result = client.send("inboundEndpoint", "some data", props);
        assertNotNull(result);
        assertEquals("got it!", result.getPayloadAsString());

        final Object foo = result.getProperty("foo", PropertyScope.INBOUND);
        assertNotNull(foo);
        assertEquals("bar", foo);
    }
}
// END SNIPPET: full-class
