package org.jboss.tools.openshift.express.internal.core.portforward;

import java.io.IOException;
import java.util.List;

import org.jboss.tools.common.databinding.ObservablePojo;

import com.jcraft.jsch.JSchException;
import com.openshift.client.IApplication;
import com.openshift.client.IApplicationPortForwarding;
import com.openshift.client.OpenShiftSSHOperationException;

public class ApplicationPortForwardingWizardModel extends ObservablePojo {

	public static final String PROPERTY_USE_DEFAULT_LOCAL_IP_ADDRESS = "useDefaultLocalIpAddress";
	
	public static final String PROPERTY_FORWARDABLE_PORTS = "forwardablePorts";
	
	private Boolean useDefaultLocalIpAddress = Boolean.TRUE;
	
	private final IApplication application;
	
	public ApplicationPortForwardingWizardModel(final IApplication application) {
		this.application = application;
	}

	/**
	 * @return the application
	 */
	public final IApplication getApplication() {
		return application;
	}

	/**
	 * @return the useDefaultLocalIpAddress
	 */
	public final Boolean getUseDefaultLocalIpAddress() {
		return useDefaultLocalIpAddress;
	}

	
	public List<IApplicationPortForwarding> getForwardablePorts() throws OpenShiftSSHOperationException {
		return application.getForwardablePorts();
	}
	
	/**
	 * @param useDefaultLocalIpAddress the useDefaultLocalIpAddress to set
	 * @throws IOException 
	 * @throws JSchException 
	 */
	public final void setUseDefaultLocalIpAddress(final Boolean useDefaultLocalIpAddress) throws OpenShiftSSHOperationException {
		updateLocalBindingValues(useDefaultLocalIpAddress);
		firePropertyChange(PROPERTY_USE_DEFAULT_LOCAL_IP_ADDRESS, this.useDefaultLocalIpAddress,
				this.useDefaultLocalIpAddress = useDefaultLocalIpAddress);
	}

	private void updateLocalBindingValues(final boolean useLocalIpAddress) throws OpenShiftSSHOperationException {
		for (IApplicationPortForwarding port : application.getForwardablePorts()) {
			port.setLocalAddress(useLocalIpAddress ? "127.0.0.1" : port.getRemoteAddress());
			if (port.getLocalPort() == null) {
				port.setLocalPort(port.getRemotePort());
			}
		}		
	}

	public void loadForwardablePorts() throws OpenShiftSSHOperationException {
		getApplication().getForwardablePorts();
		refreshForwardablePorts();
	}
	
	public void refreshForwardablePorts() throws OpenShiftSSHOperationException {
		application.refreshForwardablePorts();
		updateLocalBindingValues(this.useDefaultLocalIpAddress);
	}

	
	
	
}
