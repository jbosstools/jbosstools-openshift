package org.jboss.tools.openshift.express.internal.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.ide.eclipse.as.ui.dialogs.RequiredCredentialsDialog;
import org.jboss.tools.openshift.express.internal.core.console.IPasswordPrompter;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.osgi.framework.BundleContext;

import com.openshift.express.client.IUser;

/**
 * The activator class controls the plug-in life cycle
 */
public class OpenShiftUIActivator extends AbstractUIPlugin implements IPasswordPrompter {

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
		UserModel.setPasswordPrompt(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		UserModel.getDefault().save();
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
		return new Status(IStatus.CANCEL, OpenShiftUIActivator.PLUGIN_ID, message);
	}

	public static IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, message);
	}

	public static IStatus createErrorStatus(String message, Throwable throwable) {
		return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, message, throwable);
	}

	public static IStatus createErrorStatus(String message, Throwable throwable, Object... arguments) {
		return createErrorStatus(NLS.bind(message, arguments), throwable);
	}

	
	/**
	 * Creates an image by loading it from a file in the plugin's images
	 * directory.
	 * 
	 * @param imagePath path to the image, relative to the /icons directory of the plugin
	 * @return The image object loaded from the image file
	 */
	public final Image createImage(final String imagePath) {
		return createImageDescriptor(imagePath).createImage();
	}
	
	/**
	 * Creates an image descriptor by loading it from a file in the plugin's images
	 * directory.
	 * 
	 * @param imagePath path to the image, relative to the /icons directory of the plugin
	 * @return The image object loaded from the image file
	 */
	public final ImageDescriptor createImageDescriptor(final String imagePath) {
		IPath imageFilePath = new Path("/icons/" + imagePath);
		URL imageFileUrl = FileLocator.find(this.getBundle(), imageFilePath, null);
		return ImageDescriptor.createFromURL(imageFileUrl);
	}

	public String getPasswordFor(final IUser user) {
		final String[] val =new String[1];
		val[0] = null;
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Shell shell = Display.getDefault().getActiveShell();
				RequiredCredentialsDialog d = new RequiredCredentialsDialog(shell, user.getRhlogin(), user.getPassword());
				d.setCanModifyUser(false);
				d.setDescription("Provide enter the password for your express server");
				if( d.open() == Window.OK) {
					val[0] = d.getPass();
				}
			}
		});
		return val[0];
	}
}
