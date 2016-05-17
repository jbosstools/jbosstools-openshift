/*******************************************************************************
 * Copyright (c) 2014-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test;

import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.test.common.core.connection.ConnectionsFactoryTest;
import org.jboss.tools.openshift.test.common.core.util.StringUtilsTest;
import org.jboss.tools.openshift.test.common.core.util.URIUtilsTest;
import org.jboss.tools.openshift.test.common.ui.utils.ConnectionWizardPageModelTest;
import org.jboss.tools.openshift.test.common.ui.utils.DataBindingUtilsTest;
import org.jboss.tools.openshift.test.common.ui.utils.SmartTooltipTest;
import org.jboss.tools.openshift.test.core.LazySSLCertificateCallbackTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionPersistencyTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionRegistryTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionURLTest;
import org.jboss.tools.openshift.test.core.security.OpenShiftSecureStorageKeyTest;
import org.jboss.tools.openshift.test.core.server.OpenShiftModuleDeploymentPrefsUtilTest;
import org.jboss.tools.openshift.test.core.server.OpenShiftSubsystemTest;
import org.jboss.tools.openshift.test.core.util.ResourceUtilsTest;
import org.jboss.tools.openshift.test.core.util.ResourceUtilsIsMatchingTest;
import org.jboss.tools.openshift.test.handler.ScaleDeploymentHandlerTest;
import org.jboss.tools.openshift.test.ui.comparators.CreationTimestampComparatorTest;
import org.jboss.tools.openshift.test.ui.comparators.ProjectTreeSorterTest;
import org.jboss.tools.openshift.test.ui.comparators.ProjectViewerComparatorTest;
import org.jboss.tools.openshift.test.ui.explorer.OpenShiftExplorerContentProviderTest;
import org.jboss.tools.openshift.test.ui.explorer.OpenShiftExplorerLabelProviderTest;
import org.jboss.tools.openshift.test.ui.job.CreateApplicationFromTemplateJobTest;
import org.jboss.tools.openshift.test.ui.job.DeployImageJobTest;
import org.jboss.tools.openshift.test.ui.models.OpenShiftProjectCacheTest;
import org.jboss.tools.openshift.test.ui.portforwarding.PortForwardingWizardModelTest;
import org.jboss.tools.openshift.test.ui.property.BuildConfigPropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.ConnectionPropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.ExtTextPropertyDescriptorTest;
import org.jboss.tools.openshift.test.ui.property.ResourcePropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.WebhooksPropertySourceTest;
import org.jboss.tools.openshift.test.ui.propertytester.OpenShiftResourcePropertyTesterTest;
import org.jboss.tools.openshift.test.ui.validator.LabelKeyValidatorTest;
import org.jboss.tools.openshift.test.ui.validator.LabelValueValidatorTest;
import org.jboss.tools.openshift.test.ui.validator.PodPortValidatorTest;
import org.jboss.tools.openshift.test.ui.validator.ProjectNameValidatorTest;
import org.jboss.tools.openshift.test.ui.validator.URLValidatorTest;
import org.jboss.tools.openshift.test.ui.wizard.deployimage.PortSpecAdapterTest;
import org.jboss.tools.openshift.test.ui.wizard.newapp.NewApplicationWizardModelTest;
import org.jboss.tools.openshift.test.ui.wizard.newapp.TemplateParameterViewerUtilsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	ResourceUtilsIsMatchingTest.class,
	BuildConfigPropertySourceTest.class,
	ConnectionPropertySourceTest.class,
	ConnectionRegistryTest.class,
	ConnectionPersistencyTest.class,
	ConnectionTest.class,
	ConnectionURLTest.class,
	CreateApplicationFromTemplateJobTest.class,
	CreationTimestampComparatorTest.class,
	DataBindingUtilsTest.class,
	DeployImageJobTest.class, 
	ExtTextPropertyDescriptorTest.class,
	LabelKeyValidatorTest.class,
	LabelValueValidatorTest.class,
	ProjectNameValidatorTest.class,
	ProjectViewerComparatorTest.class,
	ProjectTreeSorterTest.class,
	LazySSLCertificateCallbackTest.class, 
	NewApplicationWizardModelTest.class,
	OpenShiftCorePreferences.class,
	OpenShiftExplorerContentProviderTest.class,
	OpenShiftExplorerLabelProviderTest.class,
	OpenShiftProjectCacheTest.class,
	OpenShiftResourcePropertyTesterTest.class,
	OpenShiftSecureStorageKeyTest.class,
	PortForwardingWizardModelTest.class,
	PortSpecAdapterTest.class,
	ResourcePropertySourceTest.class,
	ResourceUtilsTest.class,
	ScaleDeploymentHandlerTest.class,
	SmartTooltipTest.class,
	StringUtilsTest.class,
	TemplateParameterViewerUtilsTest.class,
	URIUtilsTest.class,
	URLValidatorTest.class,
	WebhooksPropertySourceTest.class,
	ConnectionWizardPageModelTest.class,
	ConnectionsFactoryTest.class,
	// triggers secure storage prompt
	//SecureStorageTest.class
	
	OpenShiftSubsystemTest.class, 
	OpenShiftModuleDeploymentPrefsUtilTest.class,
	
	PodPortValidatorTest.class
})
/**
 * @author Andre Dietisheim
 */
public class AllTestsSuite {
}
