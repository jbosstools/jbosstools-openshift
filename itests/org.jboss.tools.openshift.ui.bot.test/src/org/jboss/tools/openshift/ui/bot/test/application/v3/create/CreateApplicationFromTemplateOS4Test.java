/*******************************************************************************
 * Copyright (c) 2007-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.create;

import java.io.File;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.junit.runner.RunWith;

/**
 * 
 * @author jkopriva@redhat.com
 * 
 */
@OpenPerspective(value=JBossPerspective.class)
@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@RequiredBasicConnection
@CleanConnection
public class CreateApplicationFromTemplateOS4Test extends CreateApplicationFromTemplateTest {

	@Override
	protected String getTemplateURL() {
		return "https://raw.githubusercontent.com/jbosstools/jbosstools-openshift/master/itests/org.jboss.tools.openshift.ui.bot.test/resources/os4templates/eap72-basic-s2i-helloworld.json";
	}
	
	@Override
	protected void importTestsProject() {
		importTestsProject(new File("resources" + File.separator + "os4templates" + File.separator + TESTS_PROJECT).getAbsolutePath());
	}
	
	@Override
	protected String getFilesystemTemplatePath() {
		return new File("resources" + File.separator + "os4templates" + File.separator + TESTS_PROJECT).getAbsolutePath() + File.separator + OpenShiftResources.EAP_TEMPLATE_RESOURCES_FILENAME;
	}
	
	@Override
	protected String getWorkspaceTemplatePath() {
		return "${workspace_loc:"
				+ File.separator + TESTS_PROJECT + File.separator + OpenShiftResources.EAP_TEMPLATE_RESOURCES_FILENAME +"}";
	}
	
}
