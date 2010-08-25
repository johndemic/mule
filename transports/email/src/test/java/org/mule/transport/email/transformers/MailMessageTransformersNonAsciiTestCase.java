/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.transformers;

import org.mule.config.i18n.LocaleMessageHandler;

import java.util.Locale;

public class MailMessageTransformersNonAsciiTestCase extends MailMessageTransformersTestCase
{
    @Override
    protected String getContentType() 
    {
        return "text/plain; charset=iso-2022-jp";
    }
    
    @Override
    public Object getResultData()
    {
        return LocaleMessageHandler.getString("test-data", Locale.JAPAN,
            "MailMessageTransformersNonAsciiTestCase.getResultData", new Object[] {});
    }
}
