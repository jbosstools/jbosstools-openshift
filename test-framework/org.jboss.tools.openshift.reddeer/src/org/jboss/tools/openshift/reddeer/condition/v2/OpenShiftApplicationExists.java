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
import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
	
/**
 * Wait condition for OpenShift 2 application existence.
 *  
 * @author mlabuda@redhat.com
 */
public class OpenShiftApplicationExists extends AbstractWaitCondition {

	private String username;
	private String server;
	private String domain;
	private String application;
	
	private OpenShiftExplorerView explorer;
	
	public OpenShiftApplicationExists(String username, String server, String domain, String application) {
		this.username = username;
		this.server = server;
		this.domain = domain;
		this.application = application;
		
		explorer = new OpenShiftExplorerView();
		explorer.open();
	}

	@Override
	public boolean test() {
		explorer.activate();
		
		try {
			explorer.getOpenShift2Connection(username, server).getDomain(domain).getApplication(application);
			return true;
		} catch (RedDeerException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return "OpenShift Application with name " + application + " exists.";
	}
}
