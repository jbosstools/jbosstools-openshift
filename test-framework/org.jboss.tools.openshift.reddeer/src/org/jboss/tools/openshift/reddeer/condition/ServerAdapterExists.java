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
package org.jboss.tools.openshift.reddeer.condition;

import org.apache.commons.lang.StringUtils;
import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter;
import org.jboss.tools.openshift.reddeer.view.resources.ServerAdapter.Version;

/**
 * Wait condition to wait for a specific server adapter. To specify version of a server adapter,
 * set properly argument in constructors named isOpenShift3ServerAdapter.
 *  
 * @author mlabuda@redhat.com
 *
 */
public class ServerAdapterExists extends AbstractWaitCondition {

	private String applicationName;
	private ServerAdapter.Version version;
	private String resourceKind;
	
	/**
	 * Constructs a new Server adapter exists wait condition.
	 * 
	 * @param applicationName
	 *            name of an application that server adapter binds to
	 * @param isOpenShift3ServerAdapter
	 *            set to true if you want test OS3 server adapter, false for OS2
	 * @param resourceKind
	 *            kind of resource for which the server adapter was created (for
	 *            example Service or Deployment Config)
	 */
	public ServerAdapterExists(Version version, String applicationName, String resourceKind) {
		this.applicationName = applicationName;
		this.version = version;
		this.resourceKind = resourceKind;
	}
	
	public ServerAdapterExists(Version version, String applicationName) {
		this(version, applicationName, StringUtils.EMPTY);
	}

	@Override
	public boolean test() {
		try {
			new ServerAdapter(version, applicationName, resourceKind);
			return true;
		} catch (OpenShiftToolsException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return " server adapter exists";
	}
}
