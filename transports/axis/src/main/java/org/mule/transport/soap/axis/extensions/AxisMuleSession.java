/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.extensions;

import org.mule.api.MuleSession;

import java.util.Enumeration;

import org.apache.commons.collections.iterators.IteratorEnumeration;

/**
 * Provides an adapter to a DefaultMuleSession so that Axis can write to the session
 */
public class AxisMuleSession implements org.apache.axis.session.Session
{

    private MuleSession session;
    private Object lock = new Object();

    public AxisMuleSession(MuleSession session)
    {
        this.session = session;
    }

    public Object get(String string)
    {
        synchronized(lock)
        {
            return session.getProperty(string);
        }
    }

    public void set(String string, Object object)
    {
        synchronized(lock)
        {
            session.setProperty(string, object);
        }
    }

    public void remove(String string)
    {
        synchronized(lock)
        {
            session.removeProperty(string);
        }
    }

    public Enumeration getKeys()
    {
        synchronized(lock)
        {
            return new IteratorEnumeration(session.getPropertyNames());
        }
    }

    public void setTimeout(int i)
    {
         // TODO not supported
    }

    public int getTimeout()
    {
        return 0;
    }

    public void touch()
    {
        // nothing here to touch
    }

    public void invalidate()
    {
        synchronized(lock)
        {
            session.setValid(false);
        }
    }

    public Object getLockObject()
    {
        return lock;
    }
}
