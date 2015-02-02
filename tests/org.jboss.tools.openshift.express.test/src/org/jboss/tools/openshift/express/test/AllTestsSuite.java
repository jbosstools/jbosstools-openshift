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
package org.jboss.tools.openshift.express.test;

import org.jboss.tools.openshift.express.test.core.EmbedCartridgeStrategyTest;
import org.jboss.tools.openshift.express.test.core.OpenShiftMarkerTest;
import org.jboss.tools.openshift.express.test.core.StringsPreferenceValueTest;
import org.jboss.tools.openshift.express.test.ui.ActionFilterAdapterFactoryTest;
import org.jboss.tools.openshift.express.test.ui.explorer.ExpressExplorerContentProviderTest;
import org.jboss.tools.openshift.express.test.ui.filters.SimplePropertyActionFilterTest;
import org.jboss.tools.openshift.express.test.ui.property.ApplicationPropertySourceTest;
import org.jboss.tools.openshift.express.test.ui.utils.SSHUserConfigTest;
import org.jboss.tools.openshift.express.test.ui.wizard.application.details.ApplicationDetailsContentProviderTest;
import org.jboss.tools.openshift.express.test.ui.wizard.application.importoperation.OpenShiftMavenProfileTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
		ActionFilterAdapterFactoryTest.class,
		ApplicationDetailsContentProviderTest.class,
		ApplicationPropertySourceTest.class,
		OpenShiftMavenProfileTests.class,
		EmbedCartridgeStrategyTest.class,
		ExpressExplorerContentProviderTest.class,
		SSHUserConfigTest.class,
//		ConnectionsModelTest.class,
//		ConnectionTest.class,
//		ConnectionURLTest.class,
		OpenShiftMarkerTest.class,
		SimplePropertyActionFilterTest.class,
		StringsPreferenceValueTest.class
})
/**
 * @author Andre Dietisheim
 */
public class AllTestsSuite {
}
