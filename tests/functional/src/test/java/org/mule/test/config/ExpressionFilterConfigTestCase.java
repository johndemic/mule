/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.FunctionalTestCase;

public class ExpressionFilterConfigTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/config/expression-filter-config.xml";
    }

    public void testConfig1() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint1");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();

        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof ExpressionFilter);
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertEquals("payload-type", filter.getEvaluator());
        assertEquals("java.lang.String", filter.getExpression());
        assertNull(filter.getCustomEvaluator());
        assertFalse(filter.isNullReturnsTrue());
    }

    public void testConfig2() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint2");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();

        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof ExpressionFilter);
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertEquals("header", filter.getEvaluator());
        assertEquals("foo=bar", filter.getExpression());
        assertNull(filter.getCustomEvaluator());
        assertTrue(filter.isNullReturnsTrue());
    }

    public void testConfig3() throws Exception
    {
        EndpointBuilder eb = muleContext.getRegistry().lookupEndpointBuilder("endpoint3");
        assertNotNull(eb);

        InboundEndpoint ep = eb.buildInboundEndpoint();

        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof ExpressionFilter);
        ExpressionFilter filter = (ExpressionFilter) ep.getFilter();
        assertEquals("custom", filter.getEvaluator());
        assertEquals("a.b.c", filter.getExpression());
        assertEquals("something", filter.getCustomEvaluator());
        assertFalse(filter.isNullReturnsTrue());
    }
}
