/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.common.core;

import org.jboss.tools.usage.event.UsageEventType;
import org.jboss.tools.usage.event.UsageReporter;

public class UsageStats {

	private static final String OPENSHIFT_COMPONENT = "openshift";
	private static final int SUCCESS = 1;
	private static final int FAILURE = 0;
	private static final String HOSTTYPE_OTHER = "other";
	
	private static UsageStats INSTANCE = null;

	private UsageEventType newConnectionV2;
	private UsageEventType newApplicationV2;
	private UsageEventType importApplicationV2;
	private UsageEventType newConnectionV3;
	private UsageEventType newApplicationV3;
	private UsageEventType importApplicationV3;

	public static UsageStats getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new UsageStats();
		}
		return INSTANCE;
	}

	private UsageStats() {
		this.newConnectionV2 = createEventType(
				"new_connection_v2", // actionName
				"host type: redhat/other", // labelDescription
				null);
		this.newApplicationV2 = createEventType(
				"new_app_v2", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.importApplicationV2 = createEventType(
				"import_app_v2", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.newConnectionV3 = createEventType(
				"new_connection_v3", // actionName
				"host type: redhat/other", // labelDescription
				null);
		this.newApplicationV3 = createEventType(
				"new_app_v3", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.importApplicationV3 = createEventType(
				"import_app_v3", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
	}

	private UsageEventType createEventType(String actionName, String labelDescription, String valueDescription) {
		UsageEventType eventType = new UsageEventType(
				OPENSHIFT_COMPONENT,
				UsageEventType.getVersion(OpenShiftCommonCoreActivator.getDefault()),
				null,
				actionName,
				labelDescription,
				valueDescription);
		UsageReporter.getInstance().registerEvent(eventType);
		return eventType;
	}

	public void newV2Connection(String host) {
		UsageReporter.getInstance().countEvent(
				newConnectionV2.event(getHostType(host)));
	}

	public void newV2Application(String host, boolean success) {
		UsageReporter.getInstance().countEvent(
				newApplicationV2.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	public void importV2Application(String host, boolean success) {
		UsageReporter.getInstance().countEvent(
				importApplicationV2.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	public void newV3Connection(String host) {
		UsageReporter.getInstance().countEvent(
				newConnectionV3.event(getHostType(host)));
	}

	public void newV3Application(String host, boolean success) {
		UsageReporter.getInstance().countEvent(
				newApplicationV3.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	public void importV3Application(String host, boolean success) {
		UsageReporter.getInstance().countEvent(
				importApplicationV3.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	private static String getHostType(String host) {
		if (host == null) {
			return HOSTTYPE_OTHER;
		} 
		
		String lowercaseHost = host.toLowerCase();
		if (lowercaseHost.contains("redhat")
				|| lowercaseHost.endsWith("openshift.com")) {
			return host;
		} else {
			return HOSTTYPE_OTHER;
		}
	}
}
