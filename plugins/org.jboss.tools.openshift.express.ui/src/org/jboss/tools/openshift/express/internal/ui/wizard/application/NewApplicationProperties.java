package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.jboss.tools.openshift.express.internal.core.IApplicationProperties;

import com.openshift.client.ApplicationScale;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * Provides the properties for a new application that's not created yet
 * 
 * @author Andre Dietisheim
 */
public class NewApplicationProperties implements IApplicationProperties {

	private IOpenShiftApplicationWizardModel wizardModel;

	NewApplicationProperties(IOpenShiftApplicationWizardModel wizardModel) {
		this.wizardModel = wizardModel;
	}

	@Override
	public ApplicationScale getApplicationScale() {
		return wizardModel.getApplicationScale();
	}

	@Override
	public IStandaloneCartridge getStandaloneCartridge() {
		return wizardModel.getStandaloneCartridge();
	}

	@Override
	public String getApplicationName() {
		return wizardModel.getApplicationName();
	}
	
}