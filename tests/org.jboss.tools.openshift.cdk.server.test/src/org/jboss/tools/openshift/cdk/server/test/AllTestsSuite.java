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
package org.jboss.tools.openshift.cdk.server.test;

import org.jboss.tools.openshift.cdk.server.test.internal.CDK32LaunchControllerTest;
import org.jboss.tools.openshift.cdk.server.test.internal.CDKDockerUtilityTest;
import org.jboss.tools.openshift.cdk.server.test.internal.CDKLaunchControllerTest;
import org.jboss.tools.openshift.cdk.server.test.internal.CDKOpenshiftUtilityTest;
import org.jboss.tools.openshift.cdk.server.test.internal.CDKRegistryTest;
import org.jboss.tools.openshift.cdk.server.test.internal.CDKRuntimeDetectorTest;
import org.jboss.tools.openshift.cdk.server.test.internal.ServiceManagerParsingTest;
import org.jboss.tools.openshift.cdk.server.test.internal.VagrantPollerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ ServiceManagerParsingTest.class, CDKDockerUtilityTest.class, CDKOpenshiftUtilityTest.class,
		CDKLaunchControllerTest.class, CDK32LaunchControllerTest.class, VagrantPollerTest.class, CDKRegistryTest.class,
		CDKRuntimeDetectorTest.class })
/**
 * @author Andre Dietisheim
 */
public class AllTestsSuite {
}
