package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.openshift.express.internal.core.console.IUserModelListener;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;

import com.openshift.express.client.IUser;

public class ExpressConsoleView extends CommonNavigator implements IUserModelListener {
	protected Object getInitialInput() {
		return UserModel.getDefault();
	}
	protected CommonViewer createCommonViewer(Composite aParent) {
		CommonViewer v = super.createCommonViewer(aParent);
		UserModel.getDefault().addListener(this);
		return v;
	}
	public void dispose() {
		UserModel.getDefault().removeListener(this);
		super.dispose();
	}
	
	private void refreshViewer() {
		Display.getDefault().asyncExec(new Runnable() { 
			public void run() {
				getCommonViewer().refresh();
			}
		});
	}
	public void userAdded(IUser user) {
		if( getCommonViewer() != null && !getCommonViewer().getTree().isDisposed()) {
			refreshViewer();
		}
	}
	public void userRemoved(IUser user) {
		if( getCommonViewer() != null && !getCommonViewer().getTree().isDisposed()) {
			refreshViewer();
		}
	}
	public void userChanged(IUser user) {
		if( getCommonViewer() != null && !getCommonViewer().getTree().isDisposed()) {
			refreshViewer();
		}
	}
}
