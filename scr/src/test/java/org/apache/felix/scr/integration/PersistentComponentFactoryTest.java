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
package org.apache.felix.scr.integration;


import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleService;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.log.LogService;


@RunWith(JUnit4TestRunner.class)
public class PersistentComponentFactoryTest extends ComponentTestBase
{

    static
    {
        descriptorFile = "/integration_test_persistent_factory_components.xml";
        // uncomment to enable debugging of this test class
//        paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Test
    public void test_component_factory() throws Exception
    {
        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

        getConfigurationsDisabledThenEnable(componentname, 0, -1);

        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ComponentInstance instance = createFactoryComponentInstance(componentfactory);

        // check registered components
        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.ACTIVE);

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2
        
        checkConfigurationCount(componentname, 0, ComponentConfigurationDTO.ACTIVE);

    }


    @Test
    public void test_component_factory_disable_factory() throws Exception
    {
        // tests components remain alive after factory has been disabled

        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

        getConfigurationsDisabledThenEnable(componentname, 0, -1);

        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ComponentInstance instance = createFactoryComponentInstance(componentfactory);

        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.ACTIVE);

        // disable the factory
        disableAndCheck(componentname);
        delay();

        // factory is disabled but the instance is still alive
        TestCase.assertNotNull( SimpleComponent.INSTANCE );

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2

    }


    @Test
    public void test_component_factory_newInstance_failure() throws Exception
    {
        final String componentname = "factory.component";
        final String componentfactory = "factory.component.factory";

        getConfigurationsDisabledThenEnable(componentname, 0, -1);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        props.put( SimpleComponent.PROP_ACTIVATE_FAILURE, "Requested Failure" );
        final ComponentFactory factory = getComponentFactory(componentfactory);

        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );


        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.SATISFIED);
    }


    @Test
    public void test_component_factory_require_configuration() throws Exception
    {
        final String componentname = "factory.component.configuration";
        final String componentfactory = "factory.component.factory.configuration";

        // ensure there is no configuration for the component
        deleteConfig( componentname );
        delay();

        getConfigurationsDisabledThenEnable(componentname, 0, -1);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // At this point, since we don't have created the configuration, then the ComponentFactory
        // should not be available.
        
        checkNoFactory(componentfactory);
        
        // supply configuration now and ensure active
        configure( componentname );
        delay();        

        checkConfigurationCount(componentname, 0, ComponentConfigurationDTO.ACTIVE);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // get the component factory service
        final ComponentInstance instance = createFactoryComponentInstance(componentfactory);

        final Object instanceObject = instance.getInstance();
        TestCase.assertNotNull( instanceObject );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instanceObject );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );                        

        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.ACTIVE);

        // delete config, ensure factory is not active anymore and component instance not changed
        deleteConfig( componentname );
        delay();
        checkNoFactory(componentfactory);

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( instanceObject, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );
        TestCase.assertEquals( PROP_NAME, SimpleComponent.INSTANCE.getProperty( PROP_NAME ) );

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE ); // component is deactivated
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2

        // with removal of the factory, the created instance should also be removed
        checkConfigurationCount(componentname, 0, ComponentConfigurationDTO.ACTIVE);
    }


    @Test
    public void test_component_factory_reference() throws Exception
    {
        final String componentname = "factory.component.reference";
        final String componentfactory = "factory.component.factory.reference";

        SimpleServiceImpl.create( bundleContext, "ignored" ).setFilterProperty( "ignored" );

        getConfigurationsDisabledThenEnable(componentname, 0, -1);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        // register a service : filterprop=match
        SimpleServiceImpl match = SimpleServiceImpl.create( bundleContext, "required" ).setFilterProperty( "required" );
        delay();

        TestCase.assertNull( SimpleComponent.INSTANCE );

        final ComponentInstance instance = createFactoryComponentInstance(componentfactory);
        TestCase.assertEquals( 1, SimpleComponent.INSTANCE.m_multiRef.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCE.m_multiRef.contains( match ) );

        // check registered components
        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.ACTIVE);

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2
        checkConfigurationCount(componentname, 0, ComponentConfigurationDTO.ACTIVE);


        // overwritten filterprop
        Hashtable<String, String> propsNonMatch = new Hashtable<String, String>();
        propsNonMatch.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        propsNonMatch.put( "ref.target", "(filterprop=nomatch)" );
        ComponentFactory factory = getComponentFactory(componentfactory);
        final ComponentInstance instanceNonMatch = factory.newInstance( propsNonMatch );//works even without required reference
        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.UNSATISFIED);

        final SimpleServiceImpl noMatch = SimpleServiceImpl.create( bundleContext, "nomatch" ).setFilterProperty(
            "nomatch" );
        delay();


        TestCase.assertNotNull( instanceNonMatch.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instanceNonMatch.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );

        TestCase.assertEquals( 1, SimpleComponent.INSTANCE.m_multiRef.size() );
        TestCase.assertTrue( SimpleComponent.INSTANCE.m_multiRef.contains( noMatch ) );

        // check registered components
        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.ACTIVE);

        match.getRegistration().unregister();
        delay();

        // check registered components (ComponentFactory is still present)
        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.ACTIVE);

        //it has already been deactivated.... this should cause an exception?
        noMatch.getRegistration().unregister();
        delay();

        // check registered components (ComponentFactory is still present)
        checkConfigurationCount(componentname, 1, ComponentConfigurationDTO.UNSATISFIED);

        // deactivated due to unsatisfied reference
        TestCase.assertNull( instanceNonMatch.getInstance() );
        TestCase.assertNull( SimpleComponent.INSTANCE );

        //Check that calling dispose on a deactivated instance has no effect
        instanceNonMatch.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instanceNonMatch.getInstance() ); // SCR 112.12.6.2
    }

    @Test
    public void test_component_factory_referredTo() throws Exception
    {
        //set up the component that refers to the service the factory will create.
        final String referringComponentName = "ComponentReferringToFactoryObject";
        getConfigurationsDisabledThenEnable(referringComponentName, 1, ComponentConfigurationDTO.UNSATISFIED);

        final String componentname = "factory.component.referred";
        final String componentfactory = "factory.component.factory.referred";

        getConfigurationsDisabledThenEnable(componentname, 0, -1);
        TestCase.assertNull( SimpleComponent.INSTANCE );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( "service.pid", "myFactoryInstance" );
        final ComponentFactory factory = getComponentFactory(componentfactory);

        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertTrue( instance.getInstance() instanceof SimpleService );
        //The referring service should now be active
        checkConfigurationCount(referringComponentName, 1, ComponentConfigurationDTO.ACTIVE);

        instance.dispose();
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2

        //make sure it's unsatisfied (service is no longer available)
        checkConfigurationCount(referringComponentName, 1, ComponentConfigurationDTO.UNSATISFIED);
    }

    @Test
    public void test_component_factory_with_target_filters() throws Exception
    {
        final String componentfactory = "factory.component.reference.targetfilter";
        getConfigurationsDisabledThenEnable(componentfactory, 0, -1);

        SimpleServiceImpl s1 = SimpleServiceImpl.create(bundleContext, "service1");
        SimpleServiceImpl s2 = SimpleServiceImpl.create(bundleContext, "service2");

        // supply configuration now and ensure active
        configure( componentfactory );
        delay();        

        TestCase.assertNull( SimpleComponent.INSTANCE );
        
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );
        props.put("ref.target", "(value=service2)");
        final ComponentInstance instance = createFactoryComponentInstance(componentfactory, props);

        log.log(LogService.LOG_WARNING, "Bound Services: " +  SimpleComponent.INSTANCE.m_multiRef);
        TestCase.assertFalse( SimpleComponent.INSTANCE.m_multiRef.contains( s1 ) );
        TestCase.assertTrue( SimpleComponent.INSTANCE.m_multiRef.contains( s2 ) );

        instance.dispose();
        TestCase.assertNull( SimpleComponent.INSTANCE );
        TestCase.assertNull( instance.getInstance() ); // SCR 112.12.6.2
        
        s2.drop();
        s1.drop();
    }
    
    @Test
    public void test_component_factory_set_bundle_location_null() throws Exception
    {
        final String componentfactory = "factory.component.reference.targetfilter";
        getConfigurationsDisabledThenEnable(componentfactory, 0, -1);
        SimpleServiceImpl s1 = SimpleServiceImpl.create(bundleContext, "service1");

        ConfigurationAdmin ca = getConfigurationAdmin();
        org.osgi.service.cm.Configuration config = ca.getConfiguration( componentfactory, null );
        config.setBundleLocation( null );
        delay();
        if ( isAtLeastR5() )
        {
            //check that ConfigurationSupport got a Location changed event and set the bundle location
            TestCase.assertNotNull( config.getBundleLocation() );
        } 
        // supply configuration now and ensure active
        configure( componentfactory );
        delay();        

        TestCase.assertNull( SimpleComponent.INSTANCE );
        
        final ComponentFactory factory = getComponentFactory(componentfactory);
        
        s1.drop();
    }
    
}
