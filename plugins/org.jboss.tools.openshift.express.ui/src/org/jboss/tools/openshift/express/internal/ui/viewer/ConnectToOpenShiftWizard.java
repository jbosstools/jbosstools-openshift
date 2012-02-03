package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.eclipse.jface.wizard.Wizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPage;

public class ConnectToOpenShiftWizard extends Wizard {

	private final CredentialsWizardPage page = new CredentialsWizardPage(this);
	
	public ConnectToOpenShiftWizard() {
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public boolean performFinish() {
		if(page.performAuthentication()) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		addPage(page);
	}

}
