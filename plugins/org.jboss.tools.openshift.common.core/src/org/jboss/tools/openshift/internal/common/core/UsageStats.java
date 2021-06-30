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

	private static final String SECURE = "secure";
	private static final String UNSECURE = "not secure";

	private static UsageStats INSTANCE = null;

	private UsageEventType newConnectionV2;
	private UsageEventType newApplicationV2;
	private UsageEventType importApplicationV2;
	private UsageEventType newConnectionV3;
	private UsageEventType newApplicationV3;
	private UsageEventType importApplicationV3;

	private UsageEventType odoCli;
	private UsageEventType odoCreateComponent;
	private UsageEventType odoCreateService;
	private UsageEventType odoCreateStorage;
	private UsageEventType odoCreateUrl;
	private UsageEventType odoLogin;
	private UsageEventType odoLogout;
	private UsageEventType odoPush;
	private UsageEventType odoWatch;
	private UsageEventType odoLink;
	private UsageEventType odoDebug;
	private UsageEventType odoKubernetesVersion;
	private UsageEventType odoIsOpenshift;
	private UsageEventType odoOpenshiftVersion;

	public static UsageStats getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new UsageStats();
		}
		return INSTANCE;
	}

	private UsageStats() {
		this.newConnectionV2 = createEventType("new_connection_v2", // actionName
				"host type: redhat/other", // labelDescription
				null);
		this.newApplicationV2 = createEventType("new_app_v2", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.importApplicationV2 = createEventType("import_app_v2", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.newConnectionV3 = createEventType("new_connection_v3", // actionName
				"host type: redhat/other", // labelDescription
				null);
		this.newApplicationV3 = createEventType("new_app_v3", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.importApplicationV3 = createEventType("import_app_v3", // actionName
				"host type: redhat/other", // labelDescription
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);

		this.odoCli = createEventType("odo_command", "odo command", UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.odoCreateComponent = createEventType("odo_create_component", "component type",
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.odoCreateService = createEventType("odo_create_service", "service type",
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.odoCreateStorage = createEventType("odo_create_storage", "storage size",
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.odoCreateUrl = createEventType("odo_create_url", "secure/not secure",
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.odoLogin = createEventType("odo_login", "Number of logins",
				UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION);
		this.odoLogout = createEventType("odo_logout", "Number of logouts",
				UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION);
		this.odoPush = createEventType("odo_push", "Number of push", UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION);
		this.odoWatch = createEventType("odo_watch", "Number of watch",
				UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION);
		this.odoDebug = createEventType("odo_debug", "Number of debug",
				UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION);
		this.odoLink = createEventType("odo_link", "Number of link", UsageEventType.HOW_MANY_TIMES_VALUE_DESCRIPTION);
		this.odoKubernetesVersion = createEventType("odo_kubernetes_version", "Kubernetes version", null);
		this.odoIsOpenshift = createEventType("odo_is_openshift", "Is Cluster Openshift?",
				UsageEventType.SUCCESFULL_FAILED_VALUE_DESCRIPTION);
		this.odoOpenshiftVersion = createEventType("odo_kubernetes_version", "Openshift version", null);
	}

	private UsageEventType createEventType(String actionName, String labelDescription, String valueDescription) {
		UsageEventType eventType = new UsageEventType(OPENSHIFT_COMPONENT,
				UsageEventType.getVersion(OpenShiftCommonCoreActivator.getDefault()), null, actionName,
				labelDescription, valueDescription);
		UsageReporter.getInstance().registerEvent(eventType);
		return eventType;
	}

	public void newV2Connection(String host) {
		UsageReporter.getInstance().trackEvent(newConnectionV2.event(getHostType(host)));
	}

	public void newV2Application(String host, boolean success) {
		UsageReporter.getInstance().trackEvent(newApplicationV2.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	public void importV2Application(String host, boolean success) {
		UsageReporter.getInstance()
				.trackEvent(importApplicationV2.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	public void newV3Connection(String host) {
		UsageReporter.getInstance().trackEvent(newConnectionV3.event(getHostType(host)));
	}

	public void newV3Application(String host, boolean success) {
		UsageReporter.getInstance().trackEvent(newApplicationV3.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	public void importV3Application(String host, boolean success) {
		UsageReporter.getInstance()
				.trackEvent(importApplicationV3.event(getHostType(host), success ? SUCCESS : FAILURE));
	}

	public void odoCommand(String command, boolean success) {
		UsageReporter.getInstance().trackEvent(odoCli.event(command, success ? SUCCESS : FAILURE));
	}

	public void createComponent(String componentType, boolean success) {
		UsageReporter.getInstance().trackEvent(odoCreateComponent.event(componentType, success ? SUCCESS : FAILURE));
	}

	public void createService(String serviceType, boolean success) {
		UsageReporter.getInstance().trackEvent(odoCreateService.event(serviceType, success ? SUCCESS : FAILURE));
	}

	public void createStorage(String size, boolean success) {
		UsageReporter.getInstance().trackEvent(odoCreateStorage.event(size, success ? SUCCESS : FAILURE));
	}

	public void createURL(boolean secure, boolean success) {
		UsageReporter.getInstance()
				.trackEvent(odoCreateUrl.event(secure ? SECURE : UNSECURE, success ? SUCCESS : FAILURE));
	}

	public void login() {
		UsageReporter.getInstance().countEvent(odoLogin.event());
	}

	public void logout() {
		UsageReporter.getInstance().countEvent(odoLogout.event());
	}

	public void push() {
		UsageReporter.getInstance().countEvent(odoPush.event());
	}

	public void watch() {
		UsageReporter.getInstance().countEvent(odoWatch.event());
	}

	public void debug() {
		UsageReporter.getInstance().countEvent(odoDebug.event());
	}

	public void link() {
		UsageReporter.getInstance().countEvent(odoLink.event());
	}

	private static String getHostType(String host) {
		if (host == null) {
			return HOSTTYPE_OTHER;
		}

		String lowercaseHost = host.toLowerCase();
		if (lowercaseHost.contains("redhat") || lowercaseHost.endsWith("openshift.com")) {
			return host;
		} else {
			return HOSTTYPE_OTHER;
		}
	}

	public void kubernetesVersion(String kubernetesVersion) {
		UsageReporter.getInstance().trackEvent(odoKubernetesVersion.event(kubernetesVersion));
	}

	public void openshiftVersion(String openshiftVersion) {
		UsageReporter.getInstance().trackEvent(odoOpenshiftVersion.event(openshiftVersion));
	}

	public void isOpenShift(boolean isOpenShift) {
		UsageReporter.getInstance().trackEvent(odoIsOpenshift.event("is openshift?", isOpenShift ? SUCCESS : FAILURE));
	}
}
