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

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
public class CDKImageRegistryUrlValidatorTest extends CDKImageRegistryUrlAbstractTest {

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	/**
	 * Covers JBIDE-25121
	 */
	@Test
	public void testImageRegistryUrlValidator() {
		// test default description
		assertStringContains(wizard.getConnectionMessage(), "OpenShift server");
		// test wrong image url
		wizard.getImageRegistryUrl().setText("foo.con");
		assertStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
		// test https://
		wizard.getImageRegistryUrl().setText("https://foo.con");
		assertFalseStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
		// test http://
		wizard.getImageRegistryUrl().setText("http://foo.con");
		assertFalseStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
		// test valid image registry url from discovery feature
		discoverImageRegistryUrl();
		assertFalseStringContains(wizard.getConnectionMessage(), VALIDATION_MESSAGE);
	}

}
