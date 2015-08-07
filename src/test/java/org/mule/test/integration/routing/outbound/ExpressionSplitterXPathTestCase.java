/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExpressionSplitterXPathTestCase extends FunctionalTestCase
{

    protected ConfigVariant variant;
    protected String configResources;

    private final String MESSAGE = "<Batch xmlns=\"http://acme.com\">\n" +
            "    <Trade>\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>40000</Amount>\n" +
            "        <Currency>USD</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "    </Trade>    \n" +
            "    <Trade>\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>2000</Amount>\n" +
            "        <Currency>GBP</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "    </Trade>    \n" +
            "</Batch>";

    private final String EXPECTED_MESSAGE_1 = "<Trade xmlns=\"http://acme.com\">\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>40000</Amount>\n" +
            "        <Currency>USD</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "        <Received/>\n" +
            "    </Trade>";

    private final String EXPECTED_MESSAGE_2 = "<Trade xmlns=\"http://acme.com\">\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>2000</Amount>\n" +
            "        <Currency>GBP</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "        <Received/>\n" +
            "    </Trade>";


    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "org/mule/test/integration/routing/outbound/expression-splitter-xpath-test-flow.xml"},
            {ConfigVariant.FLOW_EL, "org/mule/test/integration/routing/outbound/expression-splitter-xpath-test-flow-el.xml"}
        });
    }

    public ExpressionSplitterXPathTestCase(ConfigVariant variant, String configResources)
    {
        this.variant = variant;
        this.configResources = configResources;
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testRecipientList() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://distributor.queue", MESSAGE, null);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(2, coll.size());
        List<?> results = (List<?>) coll.getPayload();

        assertTrue(XMLUnit.compareXML(EXPECTED_MESSAGE_1, results.get(0).toString()).identical());
        assertTrue(XMLUnit.compareXML(EXPECTED_MESSAGE_2, results.get(1).toString()).identical());
    }

    public static enum ConfigVariant
    {
        FLOW, FLOW_EL
    }

    @Override
    public String getConfigResources()
    {
        return configResources;
    }
}
