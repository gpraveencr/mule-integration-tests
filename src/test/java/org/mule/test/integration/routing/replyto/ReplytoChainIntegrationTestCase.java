/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import org.mule.config.MuleProperties;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.tck.IntegrationTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ReplytoChainIntegrationTestCase extends IntegrationTestCase
{
    protected String getConfigResources() {
        return "org/mule/test/integration/routing/replyto/injection-test.xml";
    }

    public void testReplyToChain() throws UMOException
    {
        String message = "test";
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("org/mule/test/integration/routing/replyto/injection-test.xml");

        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        UMOMessage result = client.send("vm://pojo1", message, null);
        assertNotNull(result);
        // Te
        assertEquals("Received: " + message, result.getPayload());
    }
}
