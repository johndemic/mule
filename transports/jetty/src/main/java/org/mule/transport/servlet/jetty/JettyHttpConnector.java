/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.transport.AbstractConnector;
import org.mule.transport.servlet.JarResourceServlet;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.MuleServletContextListener;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.xml.XmlConfiguration;

/**
 * The <code>JettyConnector</code> can be using to embed a Jetty server to receive requests on an 
 * http inound endpoint. One server is created for each connector declared, many Jetty endpoints 
 * can share the same connector.
 */
public class JettyHttpConnector extends AbstractConnector
{
    public static final String RESOURCE_BASE_PROPERTY = "resourceBase";

    public static final String ROOT = "/";

    public static final String JETTY = "jetty";

    private Server httpServer;

    private String configFile;

    private JettyReceiverServlet receiverServlet;

    private boolean useContinuations = false;

    protected HashMap<String, ConnectorHolder> holders = new HashMap<String, ConnectorHolder>();


    public JettyHttpConnector(MuleContext context)
    {
        super(context);
        registerSupportedProtocol("http");
        registerSupportedProtocol(JETTY);
        setInitialStateStopped(true);
    }

    public String getProtocol()
    {
        return JETTY;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        httpServer = new Server();

        if (configFile != null)
        {
            try
            {
                InputStream is = IOUtils.getResourceAsStream(configFile, getClass());
                XmlConfiguration config = new XmlConfiguration(is);
                config.configure(httpServer);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }

        try
        {
            muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>(){
                public void onNotification(MuleContextNotification notification)
                {

                    if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                    {
                        //We delay starting until the context has been started since we need the MuleAjaxServlet to initialise first
                        setInitialStateStopped(false);
                        try
                        {
                            start();
                        }
                        catch (MuleException e)
                        {
                            throw new MuleRuntimeException(CoreMessages.failedToStart(getName()), e);
                        }
                    }
                }
            });
        }
        catch (NotificationException e)
        {
            throw new InitialisationException(e, this);
        }
    }



    /**
     * Template method to dispose any resources associated with this receiver. There
     * is not need to dispose the connector as this is already done by the framework
     */
    protected void doDispose()
    {
        holders.clear();
    }

    protected void doStart() throws MuleException
    {
        try
        {
            httpServer.start();
            for (ConnectorHolder contextHolder : holders.values())
            {
                contextHolder.start();
            }
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStart("Jetty Http Receiver"), e, this);
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        try
        {
            httpServer.stop();
            for (ConnectorHolder connectorRef : holders.values())
            {
                connectorRef.stop();
            }
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStop("Jetty Http Receiver"), e, this);
        }

    }


    /**
     * Template method where any connections should be made for the connector
     *
     * @throws Exception
     */
    protected void doConnect() throws Exception
    {
        //do nothing
    }

    /**
     * Template method where any connected resources used by the connector should be
     * disconnected
     *
     * @throws Exception
     */
    protected void doDisconnect() throws Exception
    {
        //do nothing
    }

     @Override
    protected MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        MessageReceiver receiver = super.createReceiver(flowConstruct, endpoint);
        registerJettyEndpoint(receiver, endpoint);
        return receiver;
    }

    protected org.mortbay.jetty.AbstractConnector createJettyConnector()
    {
        return new SelectChannelConnector();
    }

    public void unregisterListener(MessageReceiver receiver) throws MuleException
    {
        String connectorKey = getHolderKey(receiver.getEndpoint());

        synchronized (this)
        {
            ConnectorHolder connectorRef = holders.get(connectorKey);
            if (connectorRef != null)
            {
                if (!connectorRef.isReferenced())
                {
                    getHttpServer().removeConnector(connectorRef.getConnector());
                    holders.remove(connectorKey);
                    connectorRef.stop();
                }
            }
        }
    }

    public Server getHttpServer()
    {
        return httpServer;
    }

    public String getConfigFile()
    {
        return configFile;
    }

    public void setConfigFile(String configFile)
    {
        this.configFile = configFile;
    }

    public JettyReceiverServlet getReceiverServlet()
    {
        return receiverServlet;
    }

    public void setReceiverServlet(JettyReceiverServlet receiverServlet)
    {
        this.receiverServlet = receiverServlet;
    }

    @Override
    public ReplyToHandler getReplyToHandler(ImmutableEndpoint endpoint)
    {
        if (isUseContinuations())
        {
            return new JettyContinuationsReplyToHandler(getDefaultResponseTransformers(endpoint), muleContext);
        }
        return super.getReplyToHandler(endpoint);
    }

    public boolean isUseContinuations()
    {
        return useContinuations;
    }

    public void setUseContinuations(boolean useContinuations)
    {
        this.useContinuations = useContinuations;
    }

    ConnectorHolder<? extends MuleReceiverServlet, ? extends JettyHttpMessageReceiver> registerJettyEndpoint(MessageReceiver receiver, InboundEndpoint endpoint) throws MuleException
    {

    // Make sure that there is a connector for the requested endpoint.
        String connectorKey = getHolderKey(endpoint);

        ConnectorHolder holder;

        synchronized (this)
        {
            holder = holders.get(connectorKey);
            if (holder == null)
            {
                Connector connector = createJettyConnector();

                connector.setPort(endpoint.getEndpointURI().getPort());
                connector.setHost(endpoint.getEndpointURI().getHost());
                if ("localhost".equalsIgnoreCase(endpoint.getEndpointURI().getHost()))
                {
                    logger.warn("You use localhost interface! It means that no external connections will be available."
                            + " Don't you want to use 0.0.0.0 instead (all network interfaces)?");
                }
                getHttpServer().addConnector(connector);

                holder = createContextHolder(connector, receiver.getEndpoint(), receiver);
                holders.put(connectorKey, holder);
                if(isStarted())
                {
                    holder.start();
                }
            }
            else
            {
                holder.addReceiver(receiver);
            }
        }
        return holder;
}

    protected ConnectorHolder createContextHolder(Connector connector, InboundEndpoint endpoint, MessageReceiver receiver)
    {
        return new MuleReceiverConnectorHolder(connector, (JettyReceiverServlet) createServlet(connector, endpoint), (JettyHttpMessageReceiver)receiver);
    }

    protected Servlet createServlet(Connector connector, ImmutableEndpoint endpoint)
    {
        HttpServlet servlet;
        if (getReceiverServlet() == null)
        {
            if(isUseContinuations())
            {
                servlet = new JettyContinuationsReceiverServlet();
            }
            else
            {
                servlet = new JettyReceiverServlet();
            }
        }
        else
        {
            servlet = getReceiverServlet();
        }


        String path = endpoint.getEndpointURI().getPath();
        if(StringUtils.isBlank(path))
        {
            path = ROOT;
        }

        ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
        Context context = new Context(handlerCollection, ROOT, Context.NO_SECURITY);
        context.setConnectorNames(new String[]{connector.getName()});
        context.addEventListener(new MuleServletContextListener(muleContext, getName()));

        String resourceBase = (String)endpoint.getProperty(RESOURCE_BASE_PROPERTY);
        if(resourceBase!=null)
        {

            Context resourceContext = new Context(handlerCollection, path, Context.NO_SECURITY);
            resourceContext.setResourceBase(resourceBase);
        }

        context.addServlet(JarResourceServlet.class, JarResourceServlet.DEFAULT_PATH_SPEC);

        ServletHolder holder = new ServletHolder();
        holder.setServlet(servlet);
        context.addServlet(holder, "/*");
        getHttpServer().addHandler(handlerCollection);
        return servlet;
    }

    protected String getHolderKey(ImmutableEndpoint endpoint)
    {
        return endpoint.getProtocol() + ":" + endpoint.getEndpointURI().getHost() + ":" + endpoint.getEndpointURI().getPort();
    }

    public class MuleReceiverConnectorHolder extends AbstractConnectorHolder<JettyReceiverServlet, JettyHttpMessageReceiver>
    {
        List<MessageReceiver> receivers = new ArrayList<MessageReceiver>();

        public MuleReceiverConnectorHolder(Connector connector, JettyReceiverServlet servlet, JettyHttpMessageReceiver receiver)
        {
            super(connector, servlet, receiver);
            addReceiver(receiver);
        }

        public boolean isReferenced()
        {
            return receivers.size() > 0;
        }

        public void addReceiver(JettyHttpMessageReceiver receiver)
        {

            receivers.add(receiver);
            if(started)
            {
                getServlet().addReceiver(receiver);
            }
        }

        public void removeReceiver(JettyHttpMessageReceiver receiver)
        {
            receivers.remove(receiver);
            getServlet().removeReceiver(receiver);
        }

        public void start() throws MuleException
        {
            super.start();
            
            for (MessageReceiver receiver : receivers)
            {
                servlet.addReceiver(receiver);
            }
        }

        public void stop() throws MuleException
        {
            super.stop();

            for (MessageReceiver receiver : receivers)
            {
                servlet.removeReceiver(receiver);
            }
        }
    }
}
