package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.viewer.ConnectToOpenShiftWizard;
import org.jboss.tools.openshift.express.internal.ui.viewer.OpenShiftExpressConsoleContentCategory;

import com.openshift.express.client.IUser;

public class OpenConnectionDialogActionDelegate implements IViewActionDelegate {

	private CommonNavigator view;

	@Override
	public void run(IAction action) {
		final Display display = Display.getCurrent();
		final Shell shell = new Shell(display);
		final IWizard connectToOpenShiftWizard = new ConnectToOpenShiftWizard();
		int returnCode = WizardUtils.openWizardDialog(connectToOpenShiftWizard, shell);
		if (returnCode == Window.OK) {
			Logger.debug("OpenShift Auth succeeded.");
			if (view != null) {
				IUser user = OpenShiftUIActivator.getDefault().getUser();
				//view.switchToCommonViewer();
				view.getCommonViewer().setInput(new OpenShiftExpressConsoleContentCategory(user));
				//view.getCommonViewer().refresh();
			}
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {

	}

	@Override
	public void init(IViewPart view) {
		if (view instanceof CommonNavigator) {
			this.view = (CommonNavigator) view;
		}
	}

}
