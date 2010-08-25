/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

public class ObjectToHttpClientMethodRequestTestCase extends AbstractMuleTestCase
{
    private MuleMessage setupRequestContext(String url) throws Exception
    {
        return setupRequestContext(url, "test");
    }
    
    private MuleMessage setupRequestContext(String url, String payload) throws Exception
    {
        MuleEvent event = getTestEvent(payload);
        MuleMessage message = event.getMessage();
        message.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);
        message.setOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, url);
        RequestContext.setEvent(event);
        
        return message;
    }
    
    private ObjectToHttpClientMethodRequest createTransformer() throws Exception
    {
        ObjectToHttpClientMethodRequest transformer = new ObjectToHttpClientMethodRequest();
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        return transformer;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
    }

    public void testUrlWithoutQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://localhost:8080/services");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals(null, httpMethod.getQueryString());
    }
    
    public void testUrlWithQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://localhost:8080/services?method=echo");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());
        
        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals("method=echo", httpMethod.getQueryString());
    }

    public void testUrlWithUnescapedQuery() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange");
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());
        
        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals("fruits=apple%20orange", httpMethod.getQueryString());
    }
    
    public void testAppendedUrl() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=apple%20orange");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload("test");
        
        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);
        
        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;
        
        assertEquals("fruits=apple%20orange&body=test", httpMethod.getQueryString());
    }

    public void testAppendedUrlWithExpressions() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?fruits=#[header:fruit1],#[header:fruit2]&correlationID=#[message:correlationId]");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload(NullPayload.getInstance());
        message.setCorrelationId("1234");
        message.setOutboundProperty("fruit1", "apple");
        message.setOutboundProperty("fruit2", "orange");
        
        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object response = transformer.transform(message);

        assertTrue(response instanceof HttpMethod);
        HttpMethod httpMethod = (HttpMethod) response;

        assertEquals("fruits=apple,orange&correlationID=1234", httpMethod.getQueryString());
    }

    public void testAppendedUrlWithBadExpressions() throws Exception
    {
        MuleMessage message = setupRequestContext("http://mycompany.com/test?param=#[foo:bar]}");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload(NullPayload.getInstance());
        
        ObjectToHttpClientMethodRequest transformer = createTransformer();
        
        try
        {
            transformer.transform(message);
            fail("unknown evaluator was used");
        }
        catch (TransformerException e)
        {
            //Expected
            assertTrue(e.getMessage().contains("Evaluator for \"foo\" is not registered with Mule"));
        }

        message = setupRequestContext("http://mycompany.com/test?param=#[header:bar]");
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload(NullPayload.getInstance());
        try
        {
            transformer.transform(message);
            fail("Header 'bar' not set on the message");
        }
        catch (MuleException e)
        {
            //Expected
            assertTrue(e.getCause() instanceof RequiredValueException ||
                       e.getCause().getCause() instanceof RequiredValueException);
        }
    }
    
    public void testEncodingOfParamValueTriggeredByMessageProperty() throws Exception
    {
        // the payload is already encoded, switch off encoding it in the transformer
        String encodedPayload = "encoded%20payload";
        MuleMessage message = setupRequestContext("http://mycompany.com/", encodedPayload);
        message.setOutboundProperty(HttpConnector.HTTP_ENCODE_PARAMVALUE, false);
        
        ObjectToHttpClientMethodRequest transformer = createTransformer();
        Object result = transformer.transform(message);
        
        assertTrue(result instanceof GetMethod);
        
        String expected = "body=" + encodedPayload;
        assertEquals(expected, ((GetMethod) result).getQueryString());
    }
}
