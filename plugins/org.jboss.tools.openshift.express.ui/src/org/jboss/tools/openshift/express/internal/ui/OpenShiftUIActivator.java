package org.jboss.tools.openshift.express.internal.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class OpenShiftUIActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.openshift.express.ui"; //$NON-NLS-1$

	// The shared instance
	private static OpenShiftUIActivator plugin;

	/**
	 * The constructor
	 */
	public OpenShiftUIActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		ConnectionsModel.getDefault().save();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static OpenShiftUIActivator getDefault() {
		return plugin;
	}

	public static void log(IStatus status) {
		plugin.getLog().log(status);
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
	
}
