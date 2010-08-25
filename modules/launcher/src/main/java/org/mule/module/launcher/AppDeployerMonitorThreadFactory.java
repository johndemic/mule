/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A slightly tweaked default thread factory that uses the following pattern:
 * <code>config-change-[%s]-%d-thread-%d</code>, where %s stands for application name,
 * the next number will tell one how many redeployments this app had during this container's
 * lifetime and the last digit, thread count, should always be 1. Left there for debugging
 * purposes to quickly locate any duplicate threads trying to perform a redeploy. 
 */
public class AppDeployerMonitorThreadFactory implements ThreadFactory
{

    static final AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;

    public AppDeployerMonitorThreadFactory()
    {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = String.format("app-deployer-monitor-%d-thread-",  poolNumber.getAndIncrement());
    }

    public Thread newThread(Runnable r)
    {
        Thread t = new Thread(group, r,
                              namePrefix + threadNumber.getAndIncrement(),
                              0);
        // make sure it's non-daemon, allows for an 'idle' state of Mule by preventing early termination
        t.setDaemon(false);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    }


}
