/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jboss.tools.openshift.express.core.OpenshiftCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.CredentialsPrompter;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.SSLCertificateCallback;
import org.osgi.framework.BundleContext;

/**
 * @author Andre Dietisheim
 * @author Rob Stryker
 */
public class OpenShiftUIActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.jboss.tools.openshift.express.ui"; //$NON-NLS-1$

	private static OpenShiftUIActivator plugin;

	private IPreferenceStore corePreferenceStore;

	public OpenShiftUIActivator() {
		this.corePreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, OpenShiftCoreActivator.PLUGIN_ID);
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initCoreUIIntegration();
	}

	protected void initCoreUIIntegration() {
		OpenshiftCoreUIIntegration.getDefault().setQuestionHandler(new QuestionHandler());
		OpenshiftCoreUIIntegration.getDefault().setConsoleUtility(new ConsoleUtils());
		OpenshiftCoreUIIntegration.getDefault().setCredentialPrompter(new CredentialsPrompter());
		OpenshiftCoreUIIntegration.getDefault().setSSLCertificateAuthorization(new SSLCertificateCallback());
	}

	public void stop(BundleContext context) throws Exception {
		ConnectionsModelSingleton.getInstance().save();
		plugin = null;
		super.stop(context);
	}

	public static OpenShiftUIActivator getDefault() {
		return plugin;
	}

	public static void log(IStatus status) {
		plugin.getLog().log(status);
	}

	public static void log(String message) {
		log(message, null);
	}

	public static void log(Throwable e) {
		log(e.getMessage(), e);
	}

	public static void log(String message, Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, e));
	}

	public static IStatus createCancelStatus(String message) {
		return new Status(IStatus.CANCEL, PLUGIN_ID, message);
	}

	public static IStatus createCancelStatus(String message, Object... arguments) {
		return new Status(IStatus.CANCEL, PLUGIN_ID, NLS.bind(message, arguments));
	}

	public static IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message);
	}

	public static IStatus createErrorStatus(String message, Throwable throwable) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message, throwable);
	}

	public static IStatus createErrorStatus(String message, Throwable throwable, Object... arguments) {
		return createErrorStatus(NLS.bind(message, arguments), throwable);
	}
	
	public static MultiStatus createMultiStatus(String message, Throwable t, Object... arguments) {
		MultiStatus multiStatus = new MultiStatus(PLUGIN_ID, IStatus.ERROR, NLS.bind(message, arguments), t);
		addStatuses(t, multiStatus);
		return multiStatus;
	}

	private static void addStatuses(Throwable t, MultiStatus multiStatus) {
		Throwable wrapped = getWrappedThrowable(t);
		if (wrapped != null) {
			multiStatus.add(createErrorStatus(wrapped.getMessage(), wrapped));
			addStatuses(wrapped, multiStatus);
		}
	}
	
	private static Throwable getWrappedThrowable(Throwable t) {
		if (t instanceof InvocationTargetException) {
			return ((InvocationTargetException) t).getTargetException();
		} else if (t instanceof Exception) {
			return ((Exception) t).getCause();
		}
		return null;
	}
	
    public IPreferenceStore getCorePreferenceStore() {
        // Create the preference store lazily.
        if (corePreferenceStore == null) {
        	this.corePreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, OpenShiftCoreActivator.PLUGIN_ID);

        }
        return corePreferenceStore;
    }
	
}
