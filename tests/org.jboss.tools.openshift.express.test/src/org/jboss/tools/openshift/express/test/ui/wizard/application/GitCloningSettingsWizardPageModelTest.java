/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.ui.wizard.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.GitCloningSettingsWizardPageModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftApplicationWizardModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.openshift.client.IDomain;

/**
 * @author Jeff Maury
 *
 */
public class GitCloningSettingsWizardPageModelTest {

	private GitCloningSettingsWizardPageModel model;
	
	@Mock
	private ExpressConnection connection;
	
	@Mock
	private IDomain domain;
	
	@Before
	public void setup() {
		OpenShiftApplicationWizardModel parentModel = new OpenShiftApplicationWizardModel(connection, domain);
		model = new GitCloningSettingsWizardPageModel(parentModel);
	}
	
	@Test
	public void testDefaultGitRepositoryPath() {
		assertTrue(model.isUseDefaultRepoPath());
		assertEquals(EGitUIUtils.getEGitDefaultRepositoryPath(), model.getRepositoryPath());
	}
	
    @Test
    public void testGitRepositoryPath() {
    	model.setUseDefaultRepoPath(false);
        assertFalse(model.isUseDefaultRepoPath());
        assertEquals(EGitUIUtils.getEGitDefaultRepositoryPath(), model.getRepositoryPath());
    }
    
    @Test
    public void testGitRepositoryPathAndValue() {
        model.setUseDefaultRepoPath(false);
        String path = new File(".").getAbsolutePath();
        model.setRepositoryPath(path);
        assertFalse(model.isUseDefaultRepoPath());
        assertEquals(path, model.getRepositoryPath());
    }
}
