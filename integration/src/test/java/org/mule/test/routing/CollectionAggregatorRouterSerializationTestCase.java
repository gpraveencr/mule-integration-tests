/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.serialization.SerializationException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.SimpleMemoryObjectStore;
import org.mule.runtime.core.internal.routing.EventGroup;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-13517: ignored as part of the spike. Needs review")
public class CollectionAggregatorRouterSerializationTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "collection-aggregator-router-serialization.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> registryObjects = new HashMap<>();
    registryObjects.put(BASE_IN_MEMORY_OBJECT_STORE_KEY, new EventGroupSerializerObjectStore());
    return registryObjects;
  }

  @Test
  public void eventGroupDeserialization() throws Exception {
    List<String> list = Arrays.asList("first", "second");
    flowRunner("splitter").withPayload(list).run();

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    Message request = queueHandler.read("out", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(request);
    assertThat(request.getPayload().getValue(), instanceOf(List.class));
    assertThat(((List<Message>) request.getPayload().getValue()), hasSize(list.size()));
  }

  private class EventGroupSerializerObjectStore extends SimpleMemoryObjectStore<Serializable> {

    @Override
    protected void doStore(String key, Serializable value) throws ObjectStoreException {
      if (value instanceof EventGroup) {
        value = SerializationUtils.serialize(value);
      }
      super.doStore(key, value);
    }

    @Override
    protected Serializable doRetrieve(String key) throws ObjectStoreException {
      Object value = super.doRetrieve(key);
      if (value instanceof byte[]) {
        try {
          value = SerializationUtils.deserialize((byte[]) value);
        } catch (SerializationException e) {
          // return original value
        }
      }
      return (Serializable) value;
    }
  }
}
