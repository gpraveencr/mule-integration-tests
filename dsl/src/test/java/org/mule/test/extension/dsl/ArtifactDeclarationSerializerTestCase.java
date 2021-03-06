/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.CONNECTION;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.extension.api.ExtensionConstants.EXPIRATION_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.COUNT;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.FREQUENCY;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECTION_CONFIG;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.MAX_REDELIVERY_COUNT;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.USE_SECURE_HASH;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.extensions.jms.api.connection.caching.NoCachingConfiguration;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.config.api.dsl.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ArtifactDeclarationSerializerTestCase extends AbstractElementModelTestCase {

  private String expectedAppXml;

  @Parameterized.Parameter(0)
  public String configFile;

  @Parameterized.Parameter(1)
  public ArtifactDeclaration applicationDeclaration;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"full-artifact-config-dsl-app.xml", createFullArtifactDeclaration()},
        {"multi-flow-dsl-app.xml", createMultiFlowArtifactDeclaration()},
        {"no-mule-components-dsl-app.xml", createNoMuleComponentsArtifactDeclaration()}
    });
  }

  @Before
  public void loadExpectedResult() throws IOException {
    expectedAppXml = getResourceAsString(configFile, getClass());
  }

  @Test
  public void serialize() throws Exception {
    String serializationResult = ArtifactDeclarationXmlSerializer.getDefault(dslContext).serialize(applicationDeclaration);
    compareXML(expectedAppXml, serializationResult);
  }

  @Test
  public void loadCustomConfigParameters() throws Exception {
    InputStream configIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
    ArtifactDeclarationXmlSerializer serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);

    ArtifactDeclaration artifact = serializer.deserialize(configIs);

    List<String> expectedCustomParams = asList("xmlns", "xmlns:xsi", "xsi:schemaLocation");
    List<ParameterElementDeclaration> customParameters = artifact.getCustomConfigurationParameters();
    expectedCustomParams
        .forEach(custom -> assertThat("Missing parameter: " + custom,
                                      customParameters.stream().anyMatch(p -> p.getName().equals(custom)), is(true)));
  }

  @Test
  public void loadAndserialize() throws Exception {
    InputStream configIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
    ArtifactDeclarationXmlSerializer serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);

    ArtifactDeclaration artifact = serializer.deserialize(configIs);

    String serializationResult = serializer.serialize(artifact);

    compareXML(expectedAppXml, serializationResult);
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  private static ArtifactDeclaration createMultiFlowArtifactDeclaration() {
    ElementDeclarer jms = ElementDeclarer.forExtension("JMS");
    ElementDeclarer core = ElementDeclarer.forExtension("mule");

    return newArtifact()
        .withGlobalElement(jms.newConfiguration("config")
            .withRefName("config")
            .withConnection(jms.newConnection("active-mq")
                .withParameterGroup(newParameterGroup()
                    .withParameter("cachingStrategy",
                                   newObjectValue()
                                       .ofType(NoCachingConfiguration.class.getName())
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow")
            .withRefName("send-payload")
            .withComponent(jms.newOperation("publish")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", "#[initialDestination]")
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Message")
                    .withParameter("body", "#[payload]")
                    .withParameter("properties", "#[{(initialProperty): propertyValue}]")
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("bridge")
            .withComponent(jms.newOperation("consume")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", "#[initialDestination]")
                    .withParameter("maximumWait", "1000")
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newConstruct("foreach")
                .withComponent(jms.newOperation("publish")
                    .withConfig("config")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("destination", "#[finalDestination]")
                        .getDeclaration())
                    .withParameterGroup(newParameterGroup("Message")
                        .withParameter("jmsxProperties",
                                       "#[attributes.properties.jmsxProperties]")
                        .withParameter("body",
                                       "#[bridgePrefix ++ payload]")
                        .withParameter("properties",
                                       "#[attributes.properties.userProperties]")
                        .getDeclaration())
                    .getDeclaration())
                .withComponent(core
                    .newOperation("logger")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("message", "Message Sent")
                        .getDeclaration())

                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("bridge-receiver")
            .withComponent(jms
                .newOperation("consume")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", "#[finalDestination]")
                    .withParameter("maximumWait", "1000")
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  private static ArtifactDeclaration createFullArtifactDeclaration() {

    ElementDeclarer db = ElementDeclarer.forExtension("Database");
    ElementDeclarer http = ElementDeclarer.forExtension("HTTP");
    ElementDeclarer sockets = ElementDeclarer.forExtension("Sockets");
    ElementDeclarer core = ElementDeclarer.forExtension("mule");
    ElementDeclarer wsc = ElementDeclarer.forExtension("Web Service Consumer");

    return newArtifact()
        .withCustomParameter("xmlns:doc", "http://www.mulesoft.org/schema/mule/documentation")
        .withGlobalElement(core.newConstruct("configuration")
            .withParameterGroup(group -> group.withParameter("defaultErrorHandler-ref", "referableHandler"))
            .getDeclaration())
        .withGlobalElement(core.newConstruct("errorHandler")
            .withRefName("referableHandler")
            .withComponent(core.newRoute("onErrorContinue")
                .withParameterGroup(group -> group
                    .withParameter("type", "MULE:SOURCE_RESPONSE")
                    .withParameter("logException", "false")
                    .withParameter("enableNotifications", "false"))
                .withComponent(core.newOperation("logger")
                    .withParameterGroup(group -> group
                        .withParameter("level", "TRACE"))
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(wsc.newConfiguration("config")
            .withRefName("wscConfig")
            .withParameterGroup(newParameterGroup()
                .withParameter(EXPIRATION_POLICY_PARAMETER_NAME, newObjectValue()
                    .ofType(ExpirationPolicy.class.getName())
                    .withParameter("maxIdleTime", "1")
                    .withParameter("timeUnit", MINUTES.name())
                    .build())
                .getDeclaration())
            .withConnection(wsc.newConnection("connection")
                .withParameterGroup(newParameterGroup()
                    .withParameter("soapVersion", "SOAP11")
                    .withParameter("mtomEnabled", "false")
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Connection")
                    .withParameter("wsdlLocation", "http://www.webservicex.com/globalweather.asmx?WSDL")
                    .withParameter("address", "http://www.webservicex.com/globalweather.asmx")
                    .withParameter("service", "GlobalWeather")
                    .withParameter("port", "GlobalWeatherSoap")
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(db.newConfiguration("config")
            .withRefName("dbConfig")
            .withConnection(db
                .newConnection("derby").withParameterGroup(newParameterGroup()
                    .withParameter(POOLING_PROFILE_PARAMETER_NAME, newObjectValue()
                        .withParameter("maxPoolSize", "10")
                        .build())
                    .getDeclaration())
                .withParameterGroup(newParameterGroup(CONNECTION)
                    .withParameter("connectionProperties", newObjectValue()
                        .withParameter("first", "propertyOne")
                        .withParameter("second", "propertyTwo")
                        .build())
                    .withParameter(RECONNECTION_CONFIG_PARAMETER_NAME, newObjectValue()
                        .ofType(RECONNECTION_CONFIG)
                        .withParameter("failsDeployment", "true")
                        .withParameter("reconnectionStrategy", newObjectValue()
                            .ofType(RECONNECT_ALIAS)
                            .withParameter(COUNT, "1")
                            .withParameter(FREQUENCY, "0")
                            .build())
                        .build())
                    .withParameter("database", "target/muleEmbeddedDB")
                    .withParameter("create", "true")
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(http.newConfiguration("listenerConfig")
            .withRefName("httpListener")
            .withParameterGroup(newParameterGroup()
                .withParameter("basePath", "/")
                .getDeclaration())
            .withConnection(http.newConnection("listener")
                .withParameterGroup(newParameterGroup()
                    .withParameter(TLS_PARAMETER_NAME, newObjectValue()
                        .withParameter("key-store", newObjectValue()
                            .withParameter("path", "ssltest-keystore.jks")
                            .withParameter("password", "changeit")
                            .withParameter("keyPassword", "changeit")
                            .build())
                        .withParameter("trust-store", newObjectValue()
                            .withParameter("insecure", "true")
                            .build())
                        .build())
                    .getDeclaration())
                .withParameterGroup(group -> group.withName(CONNECTION)
                    .withParameter("host", "localhost")
                    .withParameter("port", "49019")
                    .withParameter("protocol", "HTTPS"))
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(http.newConfiguration("requestConfig")
            .withRefName("httpRequester")
            .withConnection(http.newConnection("request")
                .withParameterGroup(group -> group.withParameter("authentication",
                                                                 newObjectValue()
                                                                     .ofType("org.mule.extension.http.api.request.authentication.BasicAuthentication")
                                                                     .withParameter("username", "user")
                                                                     .withParameter("password", "pass")
                                                                     .build()))
                .withParameterGroup(newParameterGroup(CONNECTION)
                    .withParameter("host", "localhost")
                    .withParameter("port", "49020")
                    .withParameter("clientSocketProperties",
                                   newObjectValue()
                                       .withParameter("connectionTimeout", "1000")
                                       .withParameter("keepAlive", "true")
                                       .withParameter("receiveBufferSize", "1024")
                                       .withParameter("sendBufferSize", "1024")
                                       .withParameter("clientTimeout", "1000")
                                       .withParameter("linger", "1000")
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow")
            .withRefName("testFlow")
            .withCustomParameter("doc:id", "docUUID")
            .withParameterGroup(group -> group.withParameter("initialState", "stopped"))
            .withComponent(http.newSource("listener")
                .withConfig("httpListener")
                .withCustomParameter("doc:id", "docUUID")
                .withParameterGroup(newParameterGroup()
                    .withParameter("path", "testBuilder")
                    .withParameter(REDELIVERY_POLICY_PARAMETER_NAME,
                                   newObjectValue()
                                       .withParameter(MAX_REDELIVERY_COUNT, "2")
                                       .withParameter(USE_SECURE_HASH, "true")
                                       .build())
                    .getDeclaration())
                .withParameterGroup(group -> group.withName(CONNECTION)
                    .withParameter(RECONNECTION_STRATEGY_PARAMETER_NAME,
                                   newObjectValue()
                                       .ofType(RECONNECT_ALIAS)
                                       .withParameter(COUNT, "1")
                                       .withParameter(FREQUENCY, "0")
                                       .build()))
                .withParameterGroup(newParameterGroup("Response")
                    .withParameter("headers", "<![CDATA[#[{{'content-type' : 'text/plain'}}]]]>")
                    .withParameter("body",
                                   "<![CDATA[#[\n"
                                       + "                    %dw 2.0\n"
                                       + "                    output application/json\n"
                                       + "                    input payload application/xml\n"
                                       + "                    var baseUrl=\"http://sample.cloudhub.io/api/v1.0/\"\n"
                                       + "                    ---\n"
                                       + "                    using (pageSize = payload.getItemsResponse.PageInfo.pageSize) {\n"
                                       + "                         links: [\n"
                                       + "                            {\n"
                                       + "                                href: fullUrl,\n"
                                       + "                                rel : \"self\"\n"
                                       + "                            }\n"
                                       + "                         ],\n"
                                       + "                         collection: {\n"
                                       + "                            size: pageSize,\n"
                                       + "                            items: payload.getItemsResponse.*Item map {\n"
                                       + "                                id: $.id,\n"
                                       + "                                type: $.type,\n"
                                       + "                                name: $.name\n"
                                       + "                            }\n"
                                       + "                         }\n"
                                       + "                    }\n"
                                       + "                ]]>")
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newConstruct("choice")
                .withRoute(core.newRoute("when")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("expression", "#[true]")
                        .getDeclaration())
                    .withComponent(db.newOperation("bulkInsert")
                        .withParameterGroup(newParameterGroup("Query")
                            .withParameter("sql",
                                           "INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)")
                            .withParameter("parameterTypes",
                                           newListValue()
                                               .withValue(newObjectValue()
                                                   .withParameter("key", "name")
                                                   .withParameter("type", "VARCHAR")
                                                   .build())
                                               .withValue(newObjectValue()
                                                   .withParameter("key", "position")
                                                   .withParameter("type", "INTEGER")
                                                   .build())
                                               .build())
                            .getDeclaration())
                        .getDeclaration())
                    .getDeclaration())
                .withRoute(core.newRoute("otherwise")
                    .withComponent(core.newConstruct("foreach")
                        .withParameterGroup(newParameterGroup()
                            .withParameter("collection", "#[myCollection]")
                            .getDeclaration())
                        .withComponent(core.newOperation("logger")
                            .withParameterGroup(newParameterGroup()
                                .withParameter("message", "#[payload]")
                                .getDeclaration())
                            .getDeclaration())
                        .getDeclaration())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(db.newOperation("bulkInsert")
                .withParameterGroup(newParameterGroup("Query")
                    .withParameter("sql", "INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)")
                    .withParameter("parameterTypes",
                                   newListValue()
                                       .withValue(newObjectValue()
                                           .withParameter("key", "name")
                                           .withParameter("type", "VARCHAR").build())
                                       .withValue(newObjectValue()
                                           .withParameter("key", "position")
                                           .withParameter("type", "INTEGER").build())
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(http.newOperation("request")
                .withConfig("httpRequester")
                .withParameterGroup(newParameterGroup("URI Settings")
                    .withParameter("path", "/nested")
                    .getDeclaration())
                .withParameterGroup(newParameterGroup()
                    .withParameter("method", "POST")
                    .getDeclaration())
                .getDeclaration())
            .withComponent(db.newOperation("insert")
                .withConfig("dbConfig")
                .withParameterGroup(newParameterGroup("Query")
                    .withParameter("sql",
                                   "INSERT INTO PLANET(POSITION, NAME, DESCRIPTION) VALUES (777, 'Pluto', :description)")
                    .withParameter("parameterTypes",
                                   newListValue()
                                       .withValue(newObjectValue()
                                           .withParameter("key", "description")
                                           .withParameter("type", "CLOB").build())
                                       .build())
                    .withParameter("inputParameters", "#[{{'description' : payload}}]")
                    .getDeclaration())
                .getDeclaration())
            .withComponent(sockets.newOperation("sendAndReceive")
                .withParameterGroup(newParameterGroup()
                    .withParameter(STREAMING_STRATEGY_PARAMETER_NAME,
                                   newObjectValue()
                                       .ofType(REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS)
                                       .withParameter("bufferSizeIncrement", "8")
                                       .withParameter("bufferUnit", "KB")
                                       .withParameter("initialBufferSize", "51")
                                       .withParameter("maxBufferSize", "1000")
                                       .build())
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Output")
                    .withParameter(TARGET_PARAMETER_NAME, "myVar")
                    .withParameter(TARGET_VALUE_PARAMETER_NAME, "#[message]")
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newConstruct("try")
                .withComponent(wsc.newOperation("consume")
                    .withParameterGroup(newParameterGroup()
                        .withParameter("operation", "GetCitiesByCountry")
                        .getDeclaration())
                    .withParameterGroup(newParameterGroup("Message")
                        .withParameter("attachments", "#[{}]")
                        .withParameter("headers",
                                       "#[{\"headers\": {con#headerIn: \"Header In Value\",con#headerInOut: \"Header In Out Value\"}]")
                        .withParameter("body", "#[payload]")
                        .getDeclaration())
                    .getDeclaration())
                .withComponent(core.newConstruct("errorHandler")
                    .withComponent(core.newRoute("onErrorContinue")
                        .withParameterGroup(newParameterGroup()
                            .withParameter("type", "MULE:ANY")
                            .getDeclaration())
                        .withComponent(core.newOperation("logger").getDeclaration())
                        .getDeclaration())
                    .withComponent(core.newRoute("onErrorPropagate")
                        .withParameterGroup(newParameterGroup()
                            .withParameter("type", "WSC:CONNECTIVITY")
                            .withParameter("when", "#[e.cause == null]")
                            .getDeclaration())
                        .getDeclaration())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("schedulerFlow")
            .withComponent(core.newSource("scheduler")
                .withParameterGroup(newParameterGroup()
                    .withParameter("schedulingStrategy", newObjectValue()
                        .ofType("org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler")
                        .withParameter("frequency", "50")
                        .withParameter("startDelay", "20")
                        .withParameter("timeUnit", "SECONDS")
                        .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newOperation("logger")
                .withParameterGroup(newParameterGroup()
                    .withParameter("message", "#[payload]").getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("cronSchedulerFlow")
            .withComponent(core.newSource("scheduler")
                .withParameterGroup(newParameterGroup()
                    .withParameter("schedulingStrategy", newObjectValue()
                        .ofType("org.mule.runtime.core.api.source.scheduler.CronScheduler")
                        .withParameter("expression", "0/1 * * * * ?")
                        .build())
                    .getDeclaration())
                .getDeclaration())
            .withComponent(core.newOperation("logger")
                .withParameterGroup(newParameterGroup()
                    .withParameter("message", "#[payload]").getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  private static Object createNoMuleComponentsArtifactDeclaration() {
    ElementDeclarer core = ElementDeclarer.forExtension("mule");
    ElementDeclarer jms = ElementDeclarer.forExtension("JMS");
    ElementDeclarer http = ElementDeclarer.forExtension("HTTP");

    return newArtifact()
        .withGlobalElement(jms.newConfiguration("config")
            .withRefName("config")
            .withConnection(jms.newConnection("active-mq")
                .withParameterGroup(newParameterGroup()
                    .withParameter("cachingStrategy",
                                   newObjectValue()
                                       .ofType(NoCachingConfiguration.class.getName())
                                       .build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(http.newConfiguration("requestConfig")
            .withRefName("httpRequester")
            .withConnection(http.newConnection("request").getDeclaration()).getDeclaration())
        .withGlobalElement(core.newConstruct("flow")
            .withRefName("send-payload")
            .withComponent(jms.newOperation("publish")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", "#[initialDestination]")
                    .getDeclaration())
                .withParameterGroup(newParameterGroup("Message")
                    .withParameter("body", "#[payload]")
                    .withParameter("properties", "#[{(initialProperty): propertyValue}]")
                    .getDeclaration())
                .getDeclaration())
            .withComponent(http.newOperation("request")
                .withConfig("httpRequester")
                .withParameterGroup(newParameterGroup("URI Settings")
                    .withParameter("path", "/nested")
                    .getDeclaration())
                .withParameterGroup(newParameterGroup()
                    .withParameter("method", "POST")
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(core.newConstruct("flow").withRefName("bridge")
            .withComponent(jms.newOperation("consume")
                .withConfig("config")
                .withParameterGroup(newParameterGroup()
                    .withParameter("destination", "#[initialDestination]")
                    .withParameter("maximumWait", "1000")
                    .getDeclaration())
                .getDeclaration())
            .withComponent(http.newOperation("request")
                .withConfig("httpRequester")
                .withParameterGroup(newParameterGroup("URI Settings")
                    .withParameter("path", "/nested")
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }
}
