package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftApplicationNotAvailableException;
import com.openshift.express.client.OpenShiftException;

@Deprecated
public class CreateNewApplicationWizardModel extends OpenShiftExpressApplicationWizardModel {

	/**
	 * Timeout in seconds when trying to contact an application after it had been created.
	 */
	private static final int APP_CREATION_TIMEOUT = 30;
	private static final String APPLICATION_NAME = "applicationName";
	private static final String APPLICATION_CARTRIDGE = "applicationCartridge";
	private static final String SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";

	public CreateNewApplicationWizardModel() {
		dataModel.put(NEW_PROJECT, true);
	}

	private void waitForAccessible(IApplication application, IProgressMonitor monitor)
			throws OpenShiftApplicationNotAvailableException, OpenShiftException {
		// monitor.subTask("waiting for application to become accessible...");
		if (!application.waitForAccessible(APP_CREATION_TIMEOUT * 1000)) {
			throw new OpenShiftApplicationNotAvailableException(NLS.bind(
					OpenShiftExpressUIMessages.HOSTNAME_NOT_ANSWERING, application.getApplicationUrl()));
		}
	}

	public IApplication createApplication(String name, ICartridge cartridge, IProgressMonitor monitor) throws OpenShiftApplicationNotAvailableException, OpenShiftException {
		IUser user = OpenShiftUIActivator.getDefault().getUser();
		if (user == null) {
			throw new OpenShiftException("Could not create application, have no valid user credentials");
		}
		IApplication application = user.createApplication(name, cartridge);
		waitForAccessible(application, monitor);
		return application;
	}
	
	public void createApplication(IProgressMonitor monitor) throws OpenShiftException {
		IApplication application = createApplication(getApplicationName(), getApplicationCartridge(), monitor);
		setApplication(application);
	}

	@Override
	public String getApplicationName() {
		return (String) dataModel.get(APPLICATION_NAME);
	}
	
	public void setApplicationName(String applicationName) {
		dataModel.put(APPLICATION_NAME, applicationName);
	}
	
	public void setApplicationCartridge(ICartridge cartridge) {
		dataModel.put(APPLICATION_CARTRIDGE, cartridge);
	}
	
	@Override
	public ICartridge getApplicationCartridge() {
		return (ICartridge)dataModel.get(APPLICATION_CARTRIDGE);
	}
	
	@Override
	public String getApplicationCartridgeName() {
		final ICartridge cartridge = (ICartridge)dataModel.get(APPLICATION_CARTRIDGE);
		if(cartridge != null) {
			return cartridge.getName();
		}
		return null;
	}

	public List<IEmbeddableCartridge> getSelectedEmbeddableCartridges() {
		@SuppressWarnings("unchecked")
		List<IEmbeddableCartridge> selectedEmbeddableCartridges = (List<IEmbeddableCartridge>) dataModel.get(SELECTED_EMBEDDABLE_CARTRIDGES);
		if(selectedEmbeddableCartridges == null) {
			selectedEmbeddableCartridges = new ArrayList<IEmbeddableCartridge>();
			dataModel.put(SELECTED_EMBEDDABLE_CARTRIDGES, selectedEmbeddableCartridges);
		}
		return selectedEmbeddableCartridges;
	}
	

}
