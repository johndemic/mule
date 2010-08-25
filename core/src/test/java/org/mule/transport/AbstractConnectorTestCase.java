/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.DefaultMuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.api.transport.MessageRequesterFactory;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

/**
 * <code>AbstractConnectorTestCase</code> tests common behaviour of all endpoints and
 * provides 'reminder' methods for implementation specific interface methods
 */
public abstract class AbstractConnectorTestCase extends AbstractMuleTestCase
{
    protected String connectorName;
    protected String encoding;

    @Override
    protected void doSetUp() throws Exception
    {
        Connector connector = createConnector();
        if (connector.getName() == null)
        {
            connector.setName("test");
        }
        connectorName = connector.getName();
        muleContext.getRegistry().registerConnector(connector);
        encoding = muleContext.getConfiguration().getDefaultEncoding();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        Connector connector = getConnector();
        if (connector.isDisposed())
        {
            fail("Connector has been disposed prematurely - lifecycle problem? Instance: " + connector);
        }
    }

    /** Look up the connector from the Registry */
    protected Connector getConnector()
    {
        return muleContext.getRegistry().lookupConnector(connectorName);
    }
    
    protected Connector getConnectorAndAssert()
    {
        Connector connector = getConnector();
        assertNotNull(connector);
        return connector;
    }

    public void testConnectorExceptionHandling() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        // Text exception handler
        Mock ehandlerMock = new Mock(SystemExceptionHandler.class, "exceptionHandler");

        ehandlerMock.expect("handleException", C.isA(Exception.class));

        assertNotNull(muleContext.getExceptionListener());
        muleContext.setExceptionListener((SystemExceptionHandler) ehandlerMock.proxy());
        connector.handleException(new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));

        if (connector instanceof AbstractConnector)
        {
            ehandlerMock.expect("handleException", C.isA(Exception.class));
            ((AbstractConnector) connector).exceptionThrown(
                    new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));
        }

        ehandlerMock.verify();

        muleContext.setExceptionListener(null);
        try
        {
            connector.handleException(new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));
            fail("Should have thrown exception as no strategy is set");
        }
        catch (RuntimeException e)
        {
            // expected
        }
    }

    public void testConnectorLifecycle() throws Exception
    {
        // this test used to use the connector created for this test, but since we need to
        // simulate disposal as well we have to create an extra instance here.

        Connector localConnector = createConnector();
        localConnector.setName(connectorName+"-temp");
        // the connector did not come from the registry, so we need to initialise manually
        localConnector.initialise();
        localConnector.start();

        assertNotNull(localConnector);
        assertTrue(localConnector.isStarted());
        assertTrue(!localConnector.isDisposed());
        localConnector.stop();
        assertTrue(!localConnector.isStarted());
        assertTrue(!localConnector.isDisposed());
        localConnector.dispose();
        assertTrue(!localConnector.isStarted());
        assertTrue(localConnector.isDisposed());

        try
        {
            localConnector.start();
            fail("Connector cannot be restarted after being disposing");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testConnectorListenerSupport() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        Service service = getTestService("anApple", Apple.class);

        InboundEndpoint endpoint = 
            muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(getTestEndpointURI());

        try
        {
            connector.registerListener(null, null, service);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(endpoint, null, service);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(null, getSensingNullMessageProcessor(), service);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        connector.registerListener(endpoint, getSensingNullMessageProcessor(), service);

        // this should work
        connector.unregisterListener(endpoint, service);
        // so should this
        try
        {
            connector.unregisterListener(null, service);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        try
        {
            connector.unregisterListener(null, service);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.unregisterListener(null, service);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        connector.unregisterListener(endpoint, service);
        muleContext.getRegistry().unregisterService(service.getName());
    }

    public void testConnectorBeanProps() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        try
        {
            connector.setName(null);
            fail("Should throw IllegalArgumentException if name set to null");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        connector.setName("Test");
        assertEquals("Test", connector.getName());

        assertNotNull("Protocol must be set as a constant", connector.getProtocol());
    }
    
    /**
     * This test only asserts that the transport descriptor mechanism works for creating the
     * MuleMessageFactory. For exhaustive tests of MuleMessageFactory implementations see
     * {@link AbstractMuleMessageFactoryTestCase} and subclasses.
     */
    public void testConnectorMuleMessageFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();
        
        MuleMessageFactory factory = connector.createMuleMessageFactory();
        assertNotNull(factory);
    }

    public void testConnectorMessageDispatcherFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        MessageDispatcherFactory factory = connector.getDispatcherFactory();
        assertNotNull(factory);
    }

    public void testConnectorMessageRequesterFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        MessageRequesterFactory factory = connector.getRequesterFactory();
        assertNotNull(factory);
    }

    public void testConnectorInitialise() throws Exception
    {
        Connector connector = getConnector();
        try
        {
            connector.initialise();
            fail("A connector cannot be initialised more than once");
        }
        catch (Exception e)
        {
            // expected
        }
    }
    
    public abstract Connector createConnector() throws Exception;

    public abstract Object getValidMessage() throws Exception;

    public abstract String getTestEndpointURI();

}
