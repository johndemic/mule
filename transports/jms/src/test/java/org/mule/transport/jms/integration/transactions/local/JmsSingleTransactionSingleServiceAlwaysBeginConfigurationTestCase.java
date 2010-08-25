/*
 * $Id: JmsSingleTransactionAlwaysBeginConfigurationTestCase.java 14304 2009-03-15 11:19:11Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration.transactions.local;

import org.mule.transport.jms.integration.AbstractJmsSingleTransactionSingleServiceTestCase;

/**
 * Test all combinations of (inbound) ALWAYS_BEGIN.  They should all pass.
 */
public class JmsSingleTransactionSingleServiceAlwaysBeginConfigurationTestCase extends
    AbstractJmsSingleTransactionSingleServiceTestCase
{
    protected String getConfigResources()
    {
        return "integration/transactions/local/jms-single-tx-single-service-always-begin.xml";
    }
}
