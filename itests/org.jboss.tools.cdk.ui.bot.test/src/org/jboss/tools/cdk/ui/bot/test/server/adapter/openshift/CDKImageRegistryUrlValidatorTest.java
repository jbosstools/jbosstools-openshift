/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.server.adapter.openshift;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.junit.Test;
import org.junit.runner.RunWith;

@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CDKImageRegistryUrlValidatorTest extends CDKImageRegistryUrlAbstractTest {

	@Override
	protected String getServerAdapter() {
		// return SERVER_ADAPTER_32;
		// workaround for https://github.com/eclipse/reddeer/issues/1841
		return "Container Development Environment 3.2";
	}
	
	/**
	 * Covers JBIDE-25121
	 */
	@Test
	public void testImageRegistryUrlValidator() {
		// test default description
		assertTrue(wizard.getConnectionMessage().contains("OpenShift server"));
		// test wrong image url
		wizard.getImageRegistryUrl().setText("foo.con");
		assertTrue(wizard.getConnectionMessage().contains(VALIDATION_MESSAGE));
		// test https://
		wizard.getImageRegistryUrl().setText("https://foo.con");
		assertFalse(wizard.getConnectionMessage().contains(VALIDATION_MESSAGE));
		// test http://
		wizard.getImageRegistryUrl().setText("http://foo.con");
		assertFalse(wizard.getConnectionMessage().contains(VALIDATION_MESSAGE));
		// test valid image registry url from discovery feature
		wizard.discover();
		assertFalse(wizard.getConnectionMessage().contains(VALIDATION_MESSAGE));	
	}

}
