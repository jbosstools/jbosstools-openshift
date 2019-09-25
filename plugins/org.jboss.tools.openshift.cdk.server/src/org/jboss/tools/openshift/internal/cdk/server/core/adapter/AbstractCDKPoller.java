/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.IServerStatePoller2;
import org.jboss.ide.eclipse.as.core.server.IServerStatePollerType;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;

public abstract class AbstractCDKPoller implements IServerStatePoller2 {

	protected IServer server;
	protected boolean canceled, done;
	protected boolean state;
	protected boolean expectedState;
	protected PollingException aborted = null;

	@Override
	public IServer getServer() {
		return server;
	}

	@Override
	public void beginPolling(IServer server, boolean expectedState) throws PollingException {
		this.server = server;
		this.canceled = done = false;
		this.expectedState = expectedState;
		this.state = !expectedState;
		launchThread();
	}

	protected abstract void launchThread();

	protected void launchThread(String name) {
		Thread t = new Thread(this::pollerRun,name);
		t.start();
	}

	protected void pollerRun() {
		setStateInternal(false, state);
		Map<String, String> env = createEnvironment(server);
		while (aborted == null && !canceled && !done) {
			IStatus stat = onePingSafe(server, env);
			int status = stat.getSeverity();
			boolean completeUp = (status == IStatus.OK && expectedState);
			boolean completeDown = (status == IStatus.ERROR && !expectedState);
			if (completeUp || completeDown) {
				setStateInternal(true, expectedState);
			}
			try {
				Thread.sleep(700);
			} catch (InterruptedException ie) {
			} // ignore
		}
	}

	protected abstract Map<String, String> createEnvironment(IServer server);

	protected synchronized void setStateInternal(boolean done, boolean state) {
		this.done = done;
		this.state = state;
	}

	public String productName() {
		return "CDK";
	}
	
	@Override
	public IStatus getCurrentStateSynchronous(IServer server) {
		Map<String, String> env = createEnvironment(server);
		int severity = onePingSafe(server, env).getSeverity();
		if (severity == IStatus.OK) {
			return new Status(IStatus.OK, CDKCoreActivator.PLUGIN_ID, productName() + " Instance is Up");
		} else if (severity == IStatus.ERROR) {
			return new Status(IStatus.ERROR, CDKCoreActivator.PLUGIN_ID, productName() + " Instance is shutoff");
		} else {
			return new Status(IStatus.INFO, CDKCoreActivator.PLUGIN_ID, productName() + " Instance is indeterminate");
		}
	}

	protected IStatus onePingSafe(IServer server, Map<String, String> env) {
		try {
			IStatus ret = onePing(server, env);
			return ret;
		} catch (PollingException pe) {
			aborted = pe;
		} catch (TimeoutException te) {
			aborted = new PollingException(te.getMessage(), te);
		} catch (IOException ioe) {
			CDKCoreActivator.pluginLog().logError(ioe.getMessage(), ioe);
		}
		return StatusFactory.infoStatus(CDKCoreActivator.PLUGIN_ID,
				"Response status indicates " + productName() + " is starting.");
	}

	protected abstract IStatus onePing(IServer server, Map<String, String> env)
			throws PollingException, IOException, TimeoutException;

	@Override
	public synchronized boolean isComplete() throws PollingException, RequiresInfoException {
		return done;
	}

	@Override
	public synchronized boolean getState() throws PollingException, RequiresInfoException {
		return state;
	}

	@Override
	public void cleanup() {
	}

	@Override
	public synchronized void cancel(int type) {
		canceled = true;
	}

	/**
	 * This is a non-interface method bc the interface method getCurrentStateSynchronous
	 * does not throw PollingException :( 
	 * @return
	 */
	public PollingException getPollingException() {
		return aborted;
	}

	@Override
	public void provideCredentials(Properties credentials) {
		// TODO Auto-generated method stub

	}

	@Override
	public IServerStatePollerType getPollerType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPollerType(IServerStatePollerType type) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getRequiredProperties() {
		// TODO Auto-generated method stub
		return null;
	}

}
