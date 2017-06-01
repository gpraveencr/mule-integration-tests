/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.junit.Assert.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class AbstractSchemaValidationTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(AbstractSchemaValidationTestCase.class);

  public static final String SEPARATOR = " ";
  protected Map schemas = new HashedMap();

  // we define these locally so that tests use the latest version rather than grabbing xsi:location
  @Before
  public void setUpSchemas() {
    addSchema("http://www.mulesoft.org/schema/mule/core", "META-INF/mule.xsd");
  }

  protected void addSchema(String name, String location) {
    schemas.put(name, location);
  }

  protected Source[] getSchemasAsSources() throws IOException {
    Source[] sources = new Source[schemas.size()];
    int index = 0;
    for (Iterator keys = schemas.keySet().iterator(); keys.hasNext();) {
      String name = (String) keys.next();
      String location = (String) schemas.get(name);
      sources[index++] = load(location);
    }
    return sources;
  }

  protected void doTest(String config) throws SAXException, IOException {
    try {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
      Schema schema = schemaFactory.newSchema(getSchemasAsSources());
      schema.newValidator().validate(load(config));
    } catch (SAXParseException ex) {
      System.err.println(MessageFormat.format("SAX parsing exception occurs at line {0}, column {1}", ex.getLineNumber(),
                                              ex.getColumnNumber()));
      throw ex;
    }
  }

  protected Source load(String name) throws IOException {
    InputStream stream = IOUtils.getResourceAsStream(name, getClass());
    assertNotNull("Cannot load " + name, stream);
    return new StreamSource(stream);
  }

  @Test
  public void testSchemaLocations() throws IOException {
    for (Iterator keys = schemas.keySet().iterator(); keys.hasNext();) {
      String name = (String) keys.next();
      String location = (String) schemas.get(name);
      LOGGER.debug("checking " + location + " for " + name);
      InputStream stream = IOUtils.getResourceAsStream(location, getClass());
      assertNotNull("Cannot load " + location + " for " + name, stream);
      stream.close();
    }
  }

}
