/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.dockerutils;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IRegistryAccount;
import org.eclipse.linuxtools.internal.docker.core.DefaultImagePushProgressHandler;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * {@link Job} to push an image from a Docker daemon into the OpenShift registry
 */
public class PushImageToRegistryJob extends AbstractDelegatingMonitorJob {

	private final IDockerConnection dockerConnection;

	private final IRegistryAccount registryAccount;
	
	private final String imageName;
	
	private final String openshiftProject;
	
	/**
	 * Constructor
	 * @param dockerConnection the Docker connection to use
	 * @param registryAccount the registry account to push the image into 
	 * @param openshiftProject the name of the OpenShift project, because the image has to be into the same namespace
	 * @param imageName the name of the image
	 */
	public PushImageToRegistryJob(final IDockerConnection dockerConnection, final IRegistryAccount registryAccount, final String openshiftProject, final String imageName) {
		super("Pushing Docker image to OpenShift registry...");
		this.dockerConnection = dockerConnection;
		this.registryAccount = registryAccount;
		this.imageName = imageName;
		this.openshiftProject = openshiftProject;
	}

	@Override
	protected IStatus doRun(final IProgressMonitor monitor) {
		monitor.beginTask("Pushing image to registry", 1);
		final String tmpImageName = getPushToRegistryImageName();
		try {
			// first, we need to tag the image with the OpenShift target
			// project
			this.dockerConnection.tagImage(imageName, tmpImageName);
			// then we can push that image with the new name
			this.dockerConnection.pushImage(tmpImageName, registryAccount,
					new DefaultImagePushProgressHandler(this.dockerConnection, tmpImageName));
		// FIXME: needs more fined tuned error handling once Neon.0 is no longer supported:
		// catch (DockerException | InterruptedException e) {
		// see https://issues.jboss.org/browse/JBIDE-22764
		} catch (Exception e) {
			return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"Failed to push the selected Docker image into OpenShift registry", e);
		} finally {
			// we need to untag the image, even if the push operation failed
			try {
				this.dockerConnection.removeTag(tmpImageName);
			} catch (DockerException | InterruptedException e) {
				return new Status(IStatus.WARNING, OpenShiftUIActivator.PLUGIN_ID,
						"Pushed the selected Docker image into OpenShift registry but failed to untag it afterwards",
						e);
			}
			monitor.done();
		}
		// TODO Auto-generated method stub
		return Status.OK_STATUS;
	}

	/**
	 * @return the name used to push to the registry.
	 */
	public String getPushToRegistryImageName() {
		try {
			final String registryHostname = new URL(this.registryAccount.getServerAddress()).getHost();
			final String tmpImageName = registryHostname + '/' + this.openshiftProject + '/' + DockerImageUtils.extractImageNameAndTag(this.imageName);
			return tmpImageName;
		} catch (MalformedURLException e) {
			OpenShiftUIActivator.getDefault().getLog().log(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
					"Failed to push the selected Docker image into OpenShift registry", e));
			return null;
		}
		
	}

}
