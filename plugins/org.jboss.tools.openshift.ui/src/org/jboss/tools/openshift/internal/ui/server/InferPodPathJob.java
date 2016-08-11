/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.ServerUtil;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.PodDeploymentPathProvider;

import com.openshift.restclient.model.IService;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class InferPodPathJob extends Job implements PropertyChangeListener {
	static String POD_PATH_LOADING_MESSAGE = "loading...";

	protected ServerSettingsWizardPageModel model;

	protected IService service; //service to load pod path for. When path is loaded, service is set to null.
	protected IService last;
	protected boolean isLoaded = false;
	protected String customPodPath = null;
	protected Hashtable<String, String> cache = new Hashtable<String, String>();
	
	public InferPodPathJob(ServerSettingsWizardPageModel model) {
		super("Infer pod path");
		this.model = model;
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	@Override
	public boolean belongsTo(Object family) {
		return ServerUtil.SERVER_JOB_FAMILY.equals(family);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if(service == null) {
			return Status.OK_STATUS;
		}
		setLoadingMessage();
		while(true) {
			IService current = service;
			if(current == null) {
				isLoaded = false;
				removeLoadingMessage();
				break;
			}
			try {
				String path = loadPodPath(current);
				if(current == service) { //otherwise, the service is changed, continue
					if(!StringUtils.isEmpty(path)) {
						cache.put(getServiceKey(current), path);
					}
					setLoadedPath(path);
					break;
				}
			} catch (CoreException e) {
				// just do another attempt.
			}
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				removeLoadingMessage();
				break;
			}
		}
		return Status.OK_STATUS;
	}

	private void setLoadingMessage() {
		if(!POD_PATH_LOADING_MESSAGE.equals(model.getPodPath())) {
			customPodPath = model.getPodPath();
			model.setInferredPodPath(POD_PATH_LOADING_MESSAGE);
		}
	}

	private void removeLoadingMessage() {
		if(POD_PATH_LOADING_MESSAGE.equals(model.getPodPath())) {
			model.setInferredPodPath(customPodPath);
		}
	}

	private void setLoadedPath(String path) {
		model.setInferredPodPath(path);
		isLoaded = true;
		customPodPath = model.getPodPath();
		if(service != null) {
			last = service;
		}
		service = null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String name = evt.getPropertyName();
		boolean serviceUpdate = ServerSettingsWizardPageModel.PROPERTY_SERVICE.equals(name);
		boolean useUpdate = ServerSettingsWizardPageModel.PROPERTY_USE_INFERRED_POD_PATH.equals(name);
		if(serviceUpdate || useUpdate) {
			final IService newService = model.isUseInferredPodPath() ? model.getService() : null;
			if(this.service != newService && (this.service != null || last != newService || useUpdate)) {
	    		service = newService;
	    		isLoaded = false;
				if(newService == null || cache.containsKey(getServiceKey(newService))) {
					final String path = newService == null ? null : cache.get(getServiceKey(newService));
					//without new thread it will wait for this job to complete.
					new Thread() {
						@Override
						public void run() {
							if(path == null) {
								removeLoadingMessage();
							}else {
								setLoadedPath(path);
							}
						}
					}.start();
				} else if(this.getState() == Job.NONE) {
					schedule(300);
				}
			}
		}			
	}

	protected String getServiceKey(IService service) {
		return new StringBuilder().append(service.getNamespace()).append("/").append(service.getName()).toString();
	}

	protected String loadPodPath(IService service) throws CoreException {
		return new PodDeploymentPathProvider().load(service, (Connection)model.getConnection());
	}

	public void stop() {
		service = null;
		last = null;
		cache.clear();
	}
}
