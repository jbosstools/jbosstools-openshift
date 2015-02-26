/*******************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test;

import org.jboss.tools.openshift.test.common.core.util.StringUtilsTest;
import org.jboss.tools.openshift.test.common.core.util.URIUtilsTest;
import org.jboss.tools.openshift.test.core.LazySSLCertificateCallbackTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionRegistryTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionSerializerTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionTest;
import org.jboss.tools.openshift.test.core.connection.ConnectionURLTest;
import org.jboss.tools.openshift.test.ui.explorer.OpenShiftExplorerContentProviderTest;
import org.jboss.tools.openshift.test.ui.explorer.OpenShiftExplorerLabelProviderTest;
import org.jboss.tools.openshift.test.ui.property.BuildConfigPropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.ConnectionPropertySourceTest;
import org.jboss.tools.openshift.test.ui.property.ExtTextPropertyDescriptorTest;
import org.jboss.tools.openshift.test.ui.property.ResourcePropertySourceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	BuildConfigPropertySourceTest.class,
	ConnectionPropertySourceTest.class,
	ConnectionRegistryTest.class,
	ConnectionSerializerTest.class,
	ConnectionTest.class,
	ExtTextPropertyDescriptorTest.class,
	//TODO add back in when tests exist
	ConnectionURLTest.class,
	LazySSLCertificateCallbackTest.class, 
	OpenShiftExplorerContentProviderTest.class,
	OpenShiftExplorerLabelProviderTest.class,
	ResourcePropertySourceTest.class,
	StringUtilsTest.class,
	URIUtilsTest.class
})
/**
 * @author Andre Dietisheim
 */
public class AllTestsSuite {
}
