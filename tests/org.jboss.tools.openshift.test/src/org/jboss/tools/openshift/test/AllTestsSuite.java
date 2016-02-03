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

import org.jboss.tools.openshift.test.common.core.connection.ConnectionsFactoryTest;
import org.jboss.tools.openshift.test.common.core.util.StringUtilsTest;
import org.jboss.tools.openshift.test.common.core.util.URIUtilsTest;
import org.jboss.tools.openshift.test.common.ui.utils.ConnectionWizardPageModelTest;
import org.jboss.tools.openshift.test.core.LazySSLCertificateCallbackTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionPersistencyTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionRegistryTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionURLTest;
import org.jboss.tools.openshift.test.core.security.OpenShiftSecureStorageKeyTest;
import org.jboss.tools.openshift.test.core.util.ResourceUtilsTest;
import org.jboss.tools.openshift.test.ui.application.NewApplicationWizardModelTest;
import org.jboss.tools.openshift.test.ui.application.TemplateFilterTest;
import org.jboss.tools.openshift.test.ui.application.TemplateParameterColumnLabelProviderTest;
import org.jboss.tools.openshift.test.ui.explorer.OpenShiftExplorerContentProviderTest;
import org.jboss.tools.openshift.test.ui.explorer.OpenShiftExplorerLabelProviderTest;
import org.jboss.tools.openshift.test.ui.job.CreateApplicationFromTemplateJobTest;
import org.jboss.tools.openshift.test.ui.portforwarding.PortForwardingWizardModelTest;
import org.jboss.tools.openshift.test.ui.property.ConnectionPropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.ExtTextPropertyDescriptorTest;
import org.jboss.tools.openshift.test.ui.property.ResourcePropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.build.BuildConfigPropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.build.WebhooksPropertySourceTest;
import org.jboss.tools.openshift.test.ui.validator.LabelKeyValidatorTest;
import org.jboss.tools.openshift.test.ui.validator.LabelValueValidatorTest;
import org.jboss.tools.openshift.test.ui.validator.ProjectNameValidatorTest;
import org.jboss.tools.openshift.test.ui.wizard.deployimage.PortSpecAdapterTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	TemplateFilterTest.class,
	BuildConfigPropertySourceTest.class,
	ConnectionPropertySourceTest.class,
	ConnectionRegistryTest.class,
	ConnectionPersistencyTest.class,
	ConnectionTest.class,
	ConnectionURLTest.class,
	CreateApplicationFromTemplateJobTest.class,
	ExtTextPropertyDescriptorTest.class,
	LabelKeyValidatorTest.class,
	LabelValueValidatorTest.class,
	ProjectNameValidatorTest.class,
	LazySSLCertificateCallbackTest.class, 
	NewApplicationWizardModelTest.class,
	OpenShiftExplorerContentProviderTest.class,
	OpenShiftExplorerLabelProviderTest.class,
	OpenShiftSecureStorageKeyTest.class,
	PortForwardingWizardModelTest.class,
	PortSpecAdapterTest.class,
	ResourcePropertySourceTest.class,
	ResourceUtilsTest.class,
	StringUtilsTest.class,
	TemplateParameterColumnLabelProviderTest.class,
	URIUtilsTest.class,
	WebhooksPropertySourceTest.class,
	ConnectionWizardPageModelTest.class,
	ConnectionsFactoryTest.class
	// triggers secure storage prompt
	//SecureStorageTest.class
})
/**
 * @author Andre Dietisheim
 */
public class AllTestsSuite {
}
