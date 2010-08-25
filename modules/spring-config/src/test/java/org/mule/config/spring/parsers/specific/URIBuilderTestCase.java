/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.endpoint.EndpointURI;
import org.mule.endpoint.URIBuilder;
import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

public class URIBuilderTestCase extends AbstractMuleTestCase
{

    private static final Map queries;

    static
    {
        queries = new HashMap();
        queries.put("aname", "avalue");
        queries.put("bname", "bvalue");
    }

    public void testAddressForProtocol()
    {
        URIBuilder uri = new URIBuilder(muleContext);
        uri.setProtocol("foo");
        uri.setAddress("foo://bar");
        assertEquals("foo://bar", uri.toString());
    }

    public void testAddressForMeta()
    {
        URIBuilder uri = new URIBuilder(muleContext);
        uri.setMeta("foo");
        uri.setAddress("baz://bar");
        assertEquals("foo:baz://bar", uri.toString());
    }

    public void testQueriesWithAddress()
    {
        URIBuilder uri = new URIBuilder(muleContext);
        uri.setAddress("foo://bar");
        uri.setQueryMap(queries);
        assertEquals("foo://bar?aname=avalue&bname=bvalue", uri.toString());
    }

    public void testLiteralQueries()
    {
        URIBuilder uri = new URIBuilder(muleContext);
        uri.setAddress("foo://bar?cname=cvalue");
        uri.setQueryMap(queries);
        assertEquals("foo://bar?cname=cvalue&aname=avalue&bname=bvalue", uri.toString());
    }

    public void testFromString()
    {
        URIBuilder uri = new URIBuilder("test://bar", muleContext);
        EndpointURI endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("test", endpointURI.getSchemeMetaInfo());
        uri = new URIBuilder("meta:test://bar", muleContext);
        endpointURI = uri.getEndpoint();
        assertEquals("test://bar", endpointURI.getUri().toString());
        assertEquals("meta", endpointURI.getSchemeMetaInfo());
    }

}
