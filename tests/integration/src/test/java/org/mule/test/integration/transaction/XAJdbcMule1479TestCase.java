/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import org.mule.module.client.MuleClient;

import java.util.List;

public class XAJdbcMule1479TestCase extends AbstractDerbyTestCase
{
    
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transaction/jdbc-xatransaction-1479.xml";
    }

    @Override
    protected void emptyTable() throws Exception
    {
        try
        {
            execSqlUpdate("DELETE FROM TEST");
        }
        catch (Exception e)
        {
            execSqlUpdate("CREATE TABLE TEST(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,DATA VARCHAR(255))");
        }
    }

    public void testJdbcXa() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in","test",null);
        
        for (int i = 0; i < 10; i++)
        {
            List results = execSqlQuery("SELECT * FROM TEST");
            assertEquals(0, results.size());
            
            Thread.sleep(1000);
        }
    }    

    public void testJmsXa() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in1", "test", null);
        
        List results = null;
        for (int i = 0; i < 10; i++)
        {
            results = execSqlQuery("SELECT * FROM TEST");
            if (results.size() > 0)
            {
                break;
            }
            
            Thread.sleep(1000);
        }

        assertEquals(1, results.size());
    }

}
