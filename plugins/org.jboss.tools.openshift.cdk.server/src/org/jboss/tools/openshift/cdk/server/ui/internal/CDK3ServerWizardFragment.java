package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDK3ServerWizardFragment extends CDKServerWizardFragment {

	public CDK3ServerWizardFragment() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Red Hat Container Development Environment";
		String desc = "A server adapter representing Red Hat Container Development Kit Version 3.";
		String label = "Minishift File: ";
		return createComposite(parent, handle, title, desc, label);
	}

	protected String findError() {
		if( homeDir == null || !(new File(homeDir)).exists()) {
			return "The selected file does not exist.";
		}
		if( credentials.getDomain() == null || credentials.getUser() == null) {
			return "The Container Development Environment Server Adapter requries Red Hat Access credentials.";
		}
		return null;
	}
	
	protected void browseHomeDirClicked() {
		browseHomeDirClicked(false);
	}


	protected void fillTextField() {
		if( homeDir != null ) {
			homeText.setText(homeDir);
		} else {
			homeDir = MinishiftBinaryUtility.getMinishiftLocation();
			homeText.setText(homeDir);
		}
	}
	
	
	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		exit();
		IServer s = getServerFromTaskModel();
		if( s instanceof IServerWorkingCopy ) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			swc.setAttribute(CDKServer.MINISHIFT_FILE, homeDir);
			swc.setAttribute(CDKServer.PROP_USERNAME, selectedUser);
		}
	}
	
	
}
