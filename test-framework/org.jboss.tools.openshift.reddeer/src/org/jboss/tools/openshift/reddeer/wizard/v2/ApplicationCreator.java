/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.v2;

import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * Template are easy to use. It is not required to use 3 required steps to create an application
 * and there are less parameters. It is enough to decide how to open new application wizard and 
 * what kind of an application should be created. Verification step is included as well.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class ApplicationCreator {
	
	private NewOpenShift2ApplicationWizard wizard;
	
	/**
	 * Initiates new application creation by opening new application wizard from the 
	 * given location - shell menu or explorer
	 * 
	 * @param openFromShellMenu true if wizard should be opened from shell menu File, New
	 * false otherwise (opened from OpenShift explorer)
	 */
	public ApplicationCreator(String username, String server, String domain, boolean openFromShellMenu) {
		wizard = new NewOpenShift2ApplicationWizard(username, server, domain);
		
		if (openFromShellMenu) {
			wizard.openWizardFromShellMenu();
		} else {
			wizard.openWizardFromExplorer();
		}
	}
	
	/**
	 * Simple application is based on any basic cartridge.
	 */
	public void createSimpleApplicationOnBasicCartridges(String basicCartridge,  
			String appName, boolean scalable, boolean smallGear, boolean createAdapter) {
		
		wizard.createNewApplicationOnBasicCartridge(basicCartridge, appName, scalable, smallGear,
				false, false, null, null, createAdapter, null, null, null, (String[]) null);
		
		boolean isEmbeddedCartridgeDialogShown = basicCartridge.equals(OpenShiftLabel.Cartridge.DIY) ||
				basicCartridge.equals(OpenShiftLabel.Cartridge.JENKINS) || scalable;
		
		wizard.postCreateSteps(isEmbeddedCartridgeDialogShown);
		
		wizard.verifyApplication(appName, appName);
		if (createAdapter) {
			wizard.verifyServerAdapter(appName, appName);
		}
	}
	
	/**
	 * Creates quickstart.
	 */
	public void createQuickstart(String quickstart, String appName, boolean scalable, 
			boolean smallGear, boolean createAdapter) {
				
		wizard.createQuickstart(quickstart, appName, scalable, smallGear, createAdapter, null);
		wizard.postCreateSteps(scalable);
		
		wizard.verifyApplication(appName, appName);
		if (createAdapter) {
			wizard.verifyServerAdapter(appName, appName);
		}
	}
	
	/**
	 * Creates an application on a downloadable cartridge.
	 */
	public void createApplicationOnDownloadableCartridge(String cartridgeURL,
			String appName, boolean scalable, boolean isEmbeddedDialogShown, boolean createAdapter,
			String deployProject, String... embeddedCartridges) {
		
		wizard.createNewApplicationOnDownloadableCartridge(cartridgeURL, appName, scalable, true,
				false, false, null, null, createAdapter, deployProject, null, null, (String[]) embeddedCartridges);
		wizard.postCreateSteps(isEmbeddedDialogShown);
	
		String project = (deployProject == null) ? appName : deployProject;
		wizard.verifyApplication(appName, project);
		if (createAdapter) {
			wizard.verifyServerAdapter(appName, project);
		}
	}
	
	/**
	 * Deploys existing project on basic cartridge. If project is git-based project, you can specify remote name
	 * otherwise pass null argument.
	 */
	public void deployExistingProject(String cartridge, String appName, String project,
			String gitRemote) {
		
		wizard.createNewApplicationOnBasicCartridge(cartridge, appName, false, true, false,
				false, null, null, true, project, null, gitRemote, (String[]) null);
		
		boolean isEmbeddedCartridgeDialogShown = cartridge.equals(OpenShiftLabel.Cartridge.DIY) ||
				cartridge.equals(OpenShiftLabel.Cartridge.JENKINS);
		
		wizard.postCreateSteps(isEmbeddedCartridgeDialogShown);
		
		wizard.verifyApplication(appName, project);
		wizard.verifyServerAdapter(appName, project);
	}	
}
