/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.felix.dm.test.integration.annotations;

import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.components.AdapterAnnotation.S1Impl;
import org.apache.felix.dm.test.components.AdapterAnnotation.S1ToS3AdapterAutoConfig;
import org.apache.felix.dm.test.components.AdapterAnnotation.S1ToS3AdapterAutoConfigField;
import org.apache.felix.dm.test.components.AdapterAnnotation.S1ToS3AdapterCallback;
import org.apache.felix.dm.test.components.AdapterAnnotation.S2Impl;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceRegistration;

/**
 * Use case: Verify Aspect Annotations usage.
 */
@RunWith(PaxExam.class)
public class AdapterAnnotationTest extends TestBase {
    public AdapterAnnotationTest() {
        super(true /* start test components bundle */);
    }

    /**
     * Check if an adapter gets injected with its adaptee using default auto config mode.
     * @throws Throwable 
     */
    @Test
    public void testAnnotatedAdapterAutoConfig() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, S1ToS3AdapterAutoConfig.ENSURE);
        ServiceRegistration sr2 = register(e, S1Impl.ENSURE);
        ServiceRegistration sr3 = register(e, S2Impl.ENSURE);
        e.waitForStep(3, 10000);
        e.ensure();
        sr1.unregister();
        sr2.unregister();
        sr3.unregister();
    }

    /**
     * Check if an adapter gets injected with its adaptee in a named class field.
     */
    @Test
    public void testAnnotatedAdapterAutoConfigField() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, S1ToS3AdapterAutoConfigField.ENSURE);
        ServiceRegistration sr2 = register(e, S1Impl.ENSURE);
        ServiceRegistration sr3 = register(e, S2Impl.ENSURE);
        e.waitForStep(3, 10000);
        e.ensure();
        sr1.unregister();
        sr2.unregister();
        sr3.unregister();
    }

    /**
     * Check if an adapter gets injected with its adaptee in a callback method.
     */
    @Test
    public void testAnnotatedAdapterCallback() {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, S1ToS3AdapterCallback.ENSURE);
        ServiceRegistration sr2 = register(e, S1Impl.ENSURE);
        ServiceRegistration sr3 = register(e, S2Impl.ENSURE);
        e.waitForStep(2, 10000);
        sr1.unregister();
        e.waitForStep(4, 10000);
        sr2.unregister();
        sr3.unregister();
    }
}
