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
package org.jboss.tools.openshift.reddeer.condition.v2;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.swt.impl.browser.InternalBrowser;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;

/**
 * 
 * Wait condition for successful OpenShift 2 application deployment.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class ApplicationIsDeployedSuccessfully extends AbstractWaitCondition {

	private String expectedText;
	
	private OpenShiftExplorerView explorer;
	
	public ApplicationIsDeployedSuccessfully(String username, String server, String domain, String appName, 
			String expectedText) {
		this.expectedText = expectedText;
		
		 explorer = new OpenShiftExplorerView();
		 explorer.open();
		 
		 explorer.getOpenShift2Connection(username, server).getDomain(domain).getApplication(appName).select();
		 
		 new ContextMenu(OpenShiftLabel.ContextMenu.SHOW_IN_BROWSER).select();
	}
	
	@Override
	public boolean test() {
		InternalBrowser browser = new InternalBrowser();
		browser.refresh();
		return browser.getText().contains(expectedText);
	}

	@Override
	public String description() {
		return " OpenShift 2 application is deployed successfully";
	}
}
