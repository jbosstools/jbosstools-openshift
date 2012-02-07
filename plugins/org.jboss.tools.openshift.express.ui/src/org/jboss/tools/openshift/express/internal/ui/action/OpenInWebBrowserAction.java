package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

public class OpenInWebBrowserAction extends AbstractAction {

	/**
	 * Constructor
	 */
	public OpenInWebBrowserAction() {
		super(OpenShiftExpressUIMessages.SHOW_IN_BROWSER_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("open-browser.gif"));
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no Console/Worker existed, a new one is
	 * created, otherwise, it is displayed. {@inheritDoc}
	 */
	@Override
	public void run() {
		if (selection != null && selection instanceof ITreeSelection
				&& ((ITreeSelection) selection).getFirstElement() instanceof IApplication) {
			try {
				final IApplication application = (IApplication) ((ITreeSelection) selection).getFirstElement();
				final String appName = application.getName();
				final String appUrl = application.getApplicationUrl();
				BrowserUtil.checkedCreateInternalBrowser(appUrl, appName,
						OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
			} catch (OpenShiftException e) {
				Logger.error("Failed to open OpenShift Application in a browser", e);
			}
		}
	}

}
