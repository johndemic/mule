/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.transport.tcp.TcpMessageReceiver.TcpWorker;

/**
 * An exception policy that returns null when the exception is thrown
 * 
 * @since 2.2.6
 */
public class DefaultMessageExceptionPolicy implements NextMessageExceptionPolicy
{

    /**
     * {@inheritDoc}
     */
    public Object handleException(Exception exception, TcpMessageReceiver receiver, TcpWorker worker) throws Exception
    {
        ((TcpConnector) receiver.getConnector()).getKeepAliveMonitor().removeExpirable(worker);
        return null;
    }

}
