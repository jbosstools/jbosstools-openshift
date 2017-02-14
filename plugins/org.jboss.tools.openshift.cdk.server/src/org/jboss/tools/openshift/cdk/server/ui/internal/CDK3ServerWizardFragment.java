package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDK3ServerWizardFragment extends CDKServerWizardFragment {

	
	public CDK3ServerWizardFragment() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Red Hat Container Development Environment";
		String desc = "A server adapter representing Red Hat Container Development Kit Version 3.";
		String label = "Minishift Binary: ";
		return createComposite(parent, handle, title, desc, label);
	}
	
	@Override
	protected Composite createComposite(Composite parent, IWizardHandle handle,
			String title, String desc, String homeLabel) {
		// boilerplate
		Composite main = setupComposite(parent, handle, title, desc);
		createCredentialWidgets(main);
		createHypervisorWidgets(main);
		createLocationWidgets(main, homeLabel);
		
		
		validateAndPack(main);
		return main;
	}
	
	private Combo hypervisorCombo;
	private String selectedHypervisor;
	protected void createHypervisorWidgets(Composite main) {
		// Point to file / folder to run
		Label l = new Label(main, SWT.NONE);
		l.setText("Hypervisor:");
		GridData comboData = new GridData();
		comboData.grabExcessHorizontalSpace = true;
		comboData.horizontalAlignment = SWT.FILL;
		comboData.horizontalSpan = 2;
		hypervisorCombo = new Combo(main, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
		hypervisorCombo.setLayoutData(comboData);
		hypervisorCombo.setItems(CDK3Server.getHypervisors());
		
		
		hypervisorCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int ind = hypervisorCombo.getSelectionIndex();
				if( ind != -1 )
					selectedHypervisor = hypervisorCombo.getItem(ind);
				validate();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		
		
		if( hypervisorCombo.getItems().length > 0 ) {
			hypervisorCombo.select(0);
		}
		int ind = hypervisorCombo.getSelectionIndex();
		if( ind != -1 )
			selectedHypervisor = hypervisorCombo.getItem(ind);
	}
	

	protected String findError() {
		if( homeDir == null || !(new File(homeDir)).exists()) {
			return "The selected file does not exist.";
		}
		if( credentials.getDomain() == null || credentials.getUser() == null) {
			return "The Container Development Environment Server Adapter requries Red Hat Access credentials.";
		}
		if( selectedHypervisor == null ) {
			return "You must choose a hypervisor.";
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
			if (homeDir != null) {
	            homeText.setText(homeDir);
			}
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
			swc.setAttribute(CDK3Server.PROP_HYPERVISOR, selectedHypervisor);
		}
	}
	
	
}
