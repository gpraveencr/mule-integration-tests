/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.construct;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.source.StartableCompositeMessageSource;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

public class FlowConfigurationFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow.xml";
    }

    @Override
    protected boolean isDisposeManagerPerSuite()
    {
        return true;
    }

    public void testFlow() throws MuleException, Exception
    {
        SimpleFlowConstruct flow = muleContext.getRegistry().lookupObject("flow");
        assertEquals(DefaultInboundEndpoint.class, flow.getMessageSource().getClass());
        assertEquals("vm://in", ((InboundEndpoint) flow.getMessageSource()).getEndpointURI()
            .getUri()
            .toString());
        assertEquals(5, flow.getMessageProcessors().size());
        assertNotNull(flow.getExceptionListener());

        assertEquals("012xyzabc3", muleContext.getClient().send("vm://in",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());

    }

    public void testFlowCompositeSource() throws MuleException, Exception
    {
        SimpleFlowConstruct flow = muleContext.getRegistry().lookupObject("flow2");
        assertEquals(StartableCompositeMessageSource.class, flow.getMessageSource().getClass());
        assertEquals(2, flow.getMessageProcessors().size());

        assertEquals("01xyz", muleContext.getClient().send("vm://in2",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());
        assertEquals("01xyz", muleContext.getClient().send("vm://in3",
            new DefaultMuleMessage("0", muleContext)).getPayloadAsString());

    }

    public void testInOutFlow() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://inout-in", new DefaultMuleMessage("0", muleContext));
        assertEquals("0", muleContext.getClient()
            .request("vm://inout-out", RECEIVE_TIMEOUT)
            .getPayloadAsString());
    }

    public void testInOutAppendFlow() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://inout-append-in", new DefaultMuleMessage("0", muleContext));
        assertEquals("0inout", muleContext.getClient()
            .request("vm://inout-append-out", RECEIVE_TIMEOUT)
            .getPayloadAsString());
    }

    public void testSplitAggregateFlow() throws MuleException, Exception
    {
        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        FruitBowl fruitBowl = new FruitBowl(apple, banana);
        fruitBowl.addFruit(orange);

        muleContext.getClient().send("vm://split-aggregate-in",
            new DefaultMuleMessage(fruitBowl, muleContext));

        MuleMessage result = muleContext.getClient().request("vm://split-aggregate-out", RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(3, coll.size());
        List<?> results = (List<?>) coll.getPayload();

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(orange.isBitten());

        assertTrue(results.contains(apple));
        assertTrue(results.contains(banana));
        assertTrue(results.contains(orange));
    }

    public void testSplitAggregateListFlow() throws MuleException, Exception
    {
        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        FruitBowl fruitBowl = new FruitBowl(apple, banana);
        fruitBowl.addFruit(orange);

        muleContext.getClient().send("vm://split-aggregate-list-in",
            new DefaultMuleMessage(fruitBowl.getFruit(), muleContext));

        MuleMessage result = muleContext.getClient()
            .request("vm://split-aggregate-list-out", RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(3, coll.size());
        List<?> results = (List<?>) coll.getPayload();

        assertTrue(apple.isBitten());
        assertTrue(banana.isBitten());
        assertTrue(orange.isBitten());

        assertTrue(results.contains(apple));
        assertTrue(results.contains(banana));
        assertTrue(results.contains(orange));
    }

    public void testSplitFilterAggregateFlow() throws MuleException, Exception
    {
        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        FruitBowl fruitBowl = new FruitBowl(apple, banana);
        fruitBowl.addFruit(orange);

        muleContext.getClient().send("vm://split-filter-aggregate-in",
            new DefaultMuleMessage(fruitBowl, muleContext));

        MuleMessage result = muleContext.getClient().request("vm://split-filter-aggregate-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(1, coll.size());
        List<?> results = (List<?>) coll.getPayload();

        assertTrue(results.contains(apple));
        assertFalse(results.contains(banana));
        assertFalse(results.contains(orange));
    }

    public void testMessageChunkSplitAggregateFlow() throws MuleException, Exception
    {
        String payload = "";
        for (int i = 0; i < 100; i++)
        {
            payload += TEST_MESSAGE;
        }

        muleContext.getClient().send("vm://message-chunk-split-aggregate-in",
            new DefaultMuleMessage(payload, muleContext));

        MuleMessage result = muleContext.getClient().request("vm://message-chunk-split-aggregate-out",
            RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotSame(payload, result.getPayload());
        assertEquals(payload, result.getPayloadAsString());
    }

    public void testComponentsFlow() throws MuleException, Exception
    {
        MuleMessage result = muleContext.getClient().send("vm://components",
            new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        assertNotNull(result);
        assertNotSame(TEST_MESSAGE + "test", result.getPayload());
    }

    public void testWireTapFlow() throws MuleException, Exception
    {
        muleContext.getClient().send("vm://wiretap-in", new DefaultMuleMessage(TEST_MESSAGE, muleContext));

        MuleMessage result = muleContext.getClient().request("vm://wiretap-out", RECEIVE_TIMEOUT);
        MuleMessage tapResult = muleContext.getClient().request("vm://wiretap-tap", RECEIVE_TIMEOUT);

        assertNotNull(result);
        assertNotNull(tapResult);
        assertNotSame(result, tapResult);
        assertEquals(TEST_MESSAGE + "inout", result.getPayloadAsString());
        assertEquals(TEST_MESSAGE + "intap", tapResult.getPayloadAsString());
    }

    public void testResponseElement() throws MuleException, Exception
    {
        MuleMessage result = muleContext.getClient().send("vm://response",
            new DefaultMuleMessage("", muleContext));

        assertNotNull(result);
        assertEquals("abcdefghi", result.getPayloadAsString());
    }

}
