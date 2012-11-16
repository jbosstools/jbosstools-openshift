/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.portforward;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.express.internal.ui.console.ConsoleUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftSshSessionFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.openshift.client.IApplication;
import com.openshift.client.IApplicationPortForwarding;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * @author Xavier Coulon
 */
public class ApplicationPortForwardingWizardModel extends ObservablePojo {

	public static final String PROPERTY_USE_DEFAULT_LOCAL_IP_ADDRESS = "useDefaultLocalIpAddress";

	public static final String PROPERTY_FORWARDABLE_PORTS = "forwardablePorts";

	public static final String PROPERTY_USE_FREE_PORTS = "useFreePorts";

	public static final String PROPERTY_PORT_FORWARDING = "portForwarding";

	private Boolean useDefaultLocalIpAddress = Boolean.TRUE;

	private Boolean useFreePorts = Boolean.FALSE;

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
	
	public boolean isPortForwardingStarted() {
		return application.isPortFowardingStarted();
	}

	public List<IApplicationPortForwarding> getForwardablePorts() throws OpenShiftSSHOperationException {
		return application.getForwardablePorts();
	}

	public boolean hasForwardablePorts() {
		return application != null && application.getForwardablePorts() != null && !application.getForwardablePorts().isEmpty();
	}

	public boolean getPortForwarding() throws OpenShiftSSHOperationException {
		return this.application.isPortFowardingStarted();
	}

	public void startPortForwarding() throws OpenShiftSSHOperationException {
		if (this.application.isPortFowardingStarted()) {
			return;
		}

		final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
		MessageConsoleStream stream = console.newMessageStream();
		stream.println("Starting port-forwarding...");
		this.application.startPortForwarding();
		for (IApplicationPortForwarding port : this.application.getForwardablePorts()) {
			stream.println(" " + getPortStatus(port));
		}
		stream.println("done.");
		ConsoleUtils.displayConsoleView(console);
		firePropertyChange(PROPERTY_PORT_FORWARDING, false, this.application.isPortFowardingStarted());
	}

	/**
	 * @return
	 */
	private String getMessageConsoleName() {
		return "Port forwarding for application '" + application.getName() + "' (" + application.getDomain().getId()
				+ ")";
	}

	public void stopPortForwarding() throws OpenShiftSSHOperationException {
		if (!this.application.isPortFowardingStarted()) {
			return;
		}
		final MessageConsole console = ConsoleUtils.findMessageConsole(getMessageConsoleName());
		MessageConsoleStream stream = console.newMessageStream();
		stream.println("Stopping port-forwarding...");
		this.application.stopPortForwarding();
		for (IApplicationPortForwarding port : this.application.getForwardablePorts()) {
			stream.println(" " + getPortStatus(port));
		}
		stream.println("done.");
		ConsoleUtils.displayConsoleView(console);

		firePropertyChange(PROPERTY_PORT_FORWARDING, true, this.application.isPortFowardingStarted());
	}

	/**
	 * @param port
	 * @return
	 * @throws OpenShiftSSHOperationException
	 */
	private String getPortStatus(IApplicationPortForwarding port) throws OpenShiftSSHOperationException {
		return port.getName() + " " + port.getLocalAddress() + ":" + port.getLocalPort() + " -> "
				+ port.getRemoteAddress() + ":" + port.getRemotePort() + " "
				+ (port.isStarted(application.getSSHSession()) ? "(started)" : "(stopped)");
	}

	/**
	 * @return the useDefaultLocalIpAddress
	 */
	public final Boolean getUseDefaultLocalIpAddress() {
		return useDefaultLocalIpAddress;
	}

	/**
	 * @param useDefaultLocalIpAddress
	 *            the useDefaultLocalIpAddress to set
	 * @throws IOException
	 * @throws JSchException
	 */
	public final void setUseDefaultLocalIpAddress(final Boolean useDefaultLocalIpAddress)
			throws OpenShiftSSHOperationException {
		updateLocalAddressBindings(useDefaultLocalIpAddress);
		firePropertyChange(PROPERTY_USE_DEFAULT_LOCAL_IP_ADDRESS, this.useDefaultLocalIpAddress,
				this.useDefaultLocalIpAddress = useDefaultLocalIpAddress);
	}

	/**
	 * @return the useFreePorts
	 */
	public Boolean getUseFreePorts() {
		return useFreePorts;
	}

	/**
	 * @param useFreePorts
	 *            the useFreePorts to set
	 * @throws OpenShiftSSHOperationException
	 */
	public void setUseFreePorts(Boolean useFreePorts) throws OpenShiftSSHOperationException {
		// do not change the current bindings if port forwarding is already started.
		if (!application.isPortFowardingStarted()) { 
			updateLocalPortBindings(useFreePorts);
		}
		firePropertyChange(PROPERTY_USE_DEFAULT_LOCAL_IP_ADDRESS, this.useFreePorts, this.useFreePorts = useFreePorts);
	}

	private void updateLocalAddressBindings(final boolean useLocalIpAddress) throws OpenShiftSSHOperationException {
		final List<IApplicationPortForwarding> ports = application.getForwardablePorts();
		for (IApplicationPortForwarding port : ports) {
			port.setLocalAddress(useLocalIpAddress ? "127.0.0.1" : port.getRemoteAddress());
		}
	}

	private void updateLocalPortBindings(final boolean useFreePorts) throws OpenShiftSSHOperationException {
		final List<IApplicationPortForwarding> ports = application.getForwardablePorts();
		final List<String> bindings = new ArrayList<String>();
		// update local bindings while avoiding duplicates
		for (IApplicationPortForwarding port : ports) {
			if (useFreePorts) {
				// find free port for every port
				port.setLocalPort(SocketUtil.findFreePort());
			} else {
				// find duplicates and if match we find free port for those
				// until stops.
				port.setLocalPort(port.getRemotePort());
				String key = computeKey(port);
				while (bindings.contains(key)) {
					port.setLocalPort(SocketUtil.findFreePort());
					key = computeKey(port);
				}
				bindings.add(key);
			}
		}
	}

	/**
	 * @param port
	 * @return
	 */
	private String computeKey(IApplicationPortForwarding port) {
		return port.getLocalAddress() + ":" + port.getLocalPort();
	}

	public void loadForwardablePorts() throws OpenShiftSSHOperationException {
		getApplication().getForwardablePorts();
		refreshForwardablePorts();
	}

	public void refreshForwardablePorts() throws OpenShiftSSHOperationException {
		// we don't reload/refresh the ports if port forwarding is already running since
		// we then loose the existing local ip/ports.
		if (!application.isPortFowardingStarted()) { 
			application.refreshForwardablePorts();
			updateLocalAddressBindings(this.useDefaultLocalIpAddress);
			updateLocalPortBindings(this.useFreePorts);
			// force property change fire to notify wizard that things changed in the list
			firePropertyChange(PROPERTY_FORWARDABLE_PORTS, null, application.getForwardablePorts());
		}
	}

	/**
	 * @param monitor
	 * @throws OpenShiftSSHOperationException
	 * @throws JSchException
	 */
	// TODO : move this method into the WizardModel ?
	boolean verifyApplicationSSHSession() throws OpenShiftSSHOperationException {
		final boolean hasAlreadySSHSession = getApplication().hasSSHSession();
		if (!hasAlreadySSHSession) {
			Logger.debug("Opening a new SSH Session for application '" + getApplication().getName() + "'");
			final Session session = OpenShiftSshSessionFactory.getInstance().createSession(
					getApplication());
				getApplication().setSSHSession(session);
		}
		// now, check if the session is valid (ie, not null and still connected)
		return getApplication().hasSSHSession();
	}


}
