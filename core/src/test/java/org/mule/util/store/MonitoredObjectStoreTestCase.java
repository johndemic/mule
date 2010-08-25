/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreNotAvaliableException;
import org.mule.tck.AbstractMuleTestCase;

import java.io.Serializable;

public class MonitoredObjectStoreTestCase extends AbstractMuleTestCase
{
    private static final int EXPIRATION_INTERVAL = 500;
    
    public void testShutdownWithHangingExpireThread() throws Exception
    {        
        ExpiringStore store = createExpiringStore();
        
        // sleep some time for the expire to kick in
        Thread.sleep(EXPIRATION_INTERVAL * 2);
        
        // now dispose the store, this kills the expire thread 
        // that is still active, as it is a daemon thread
        store.dispose();
        
        assertTrue(store.expireStarted);
        assertFalse(store.expireFinished);
    }

    private ExpiringStore createExpiringStore() throws InitialisationException
    {
        ExpiringStore store = new ExpiringStore();
        store.setExpirationInterval(EXPIRATION_INTERVAL);
        store.initialise();
        
        return store;
    }
    
    private static class ExpiringStore extends AbstractMonitoredObjectStore<String>
    {
        protected boolean expireStarted = false;
        protected boolean expireFinished = false;
        
        public ExpiringStore()
        {
            super();
        }
        
        @Override
        protected void expire()
        {
            try
            {
                expireStarted = true;
                Thread.sleep(EXPIRATION_INTERVAL * 10);
                expireFinished = true;
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException("expire was interrupted", e);
            }
        }
        
        public boolean contains(Serializable id) throws ObjectStoreNotAvaliableException
        {
            return false;
        }

        public String remove(Serializable id) throws ObjectStoreException
        {
            return null;
        }

        public String retrieve(Serializable id) throws ObjectStoreException
        {
            return null;
        }

        public void store(Serializable id, String item) throws ObjectStoreException
        {
            // does nothing
        }
    }
}
