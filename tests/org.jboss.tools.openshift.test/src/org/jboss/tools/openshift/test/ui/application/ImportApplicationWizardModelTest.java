/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizardModel;
import org.junit.Test;
/**
 * @author Fred Bricon
 */
public class ImportApplicationWizardModelTest {

	@Test
	public void extractProjectNameFromURI() {
		assertNull(ImportApplicationWizardModel.extractProjectNameFromURI(null));
		assertEquals("bar", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/bar"));
		assertEquals("bar", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/bar.git"));
		assertEquals("bar", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/bar/"));
		assertEquals("ba.r", ImportApplicationWizardModel.extractProjectNameFromURI("http://foo/ba.r"));

		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart"));
		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart.git"));
		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart/"));
		assertEquals("quickstart", ImportApplicationWizardModel.extractProjectNameFromURI("https://github.com/akram/quickstart.git/"));

	}

}