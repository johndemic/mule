/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.transport.Connector;

/**
 * Generates consistent objects names for Mule components
 */
// @ThreadSafe
public final class ObjectNameHelper
{
    public static final String SEPARATOR = ".";
    //public static final char HASH = '#';
    public static final String CONNECTOR_PREFIX = "connector";
    public static final String ENDPOINT_PREFIX = "endpoint";

    private MuleContext muleContext;


    public ObjectNameHelper(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public String getEndpointName(final EndpointURI endpointUri)
    {
            String address = endpointUri.getAddress();
            if (StringUtils.isBlank(address))
            {
                // for some endpoints in TCK like test://xxx
                address = endpointUri.toString();
            }
            // Make sure we include the endpoint scheme in the name
            address = (address.indexOf(":/") > -1 ? address : endpointUri.getScheme()
                            + SEPARATOR + address);
            String name = ENDPOINT_PREFIX + SEPARATOR + replaceObjectNameChars(address);

            return ensureUniqueEndpoint(name);
    }

    protected String ensureUniqueEndpoint(String name)
    {
        int i = 0;
        String tempName = name;
        // Check that the generated name does not conflict with an existing global
        // endpoint.
        // We can't check local edpoints right now but the chances of conflict are
        // very small and will be
        // reported during JMX object registration
        while (muleContext.getRegistry().lookupObject(tempName) != null)
        {
            i++;
            tempName = name + SEPARATOR + i;
        }
        return tempName;
    }

    protected String ensureUniqueConnector(String name)
    {
        int i = 0;
        String tempName = name;
        // Check that the generated name does not conflict with an existing global
        // endpoint.
        // We can't check local edpoints right now but the chances of conflict are
        // very small and will be
        // reported during JMX object registration
        try
        {
            while (muleContext.getRegistry().lookupConnector(tempName) != null)
            {
                i++;
                tempName = name + SEPARATOR + i;
            }
        }
        catch (Exception e)
        {
            //ignore
        }
        return tempName;
    }

    public String getConnectorName(Connector connector)
    {
        if (connector.getName() != null && connector.getName().indexOf('#') == -1)
        {
            String name = replaceObjectNameChars(connector.getName());
            return ensureUniqueConnector(name);
        }
        else
        {
            int i = 0;
            String name = CONNECTOR_PREFIX + SEPARATOR + connector.getProtocol() + SEPARATOR + i;
            return ensureUniqueConnector(name);
        }
    }

    public String replaceObjectNameChars(String name)
    {
        String value = name.replaceAll("//", SEPARATOR);
        value = value.replaceAll("\\p{Punct}", SEPARATOR);
        value = value.replaceAll("\\" + SEPARATOR + "{2,}", SEPARATOR);
        if (value.endsWith(SEPARATOR))
        {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

}
