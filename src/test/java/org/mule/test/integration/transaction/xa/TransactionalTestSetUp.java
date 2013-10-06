/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.junit.Ignore;

@Ignore
public interface TransactionalTestSetUp
{

    /**
     * Creates resources required by the test
     * @throws Exception
     */
    void initialize() throws Exception;

    /**
     * Destroy resources created for the test
     * @throws Exception
     */
    void finalice() throws Exception;
}
