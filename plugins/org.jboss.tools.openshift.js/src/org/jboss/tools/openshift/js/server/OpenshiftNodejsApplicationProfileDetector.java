package org.jboss.tools.openshift.js.server;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.server.adapter.IOpenshiftServerAdapterProfileDetector;
import org.jboss.tools.openshift.core.server.adapter.ProjectBuilderTypeDetector;

import com.openshift.restclient.model.IResource;

public class OpenshiftNodejsApplicationProfileDetector implements IOpenshiftServerAdapterProfileDetector {
	
	public static final String PROFILE = "openshift3.nodejs";

	public OpenshiftNodejsApplicationProfileDetector() {
		super();
	}

	@Override
	public String getProfile() {
		return PROFILE;
	}

	@Override
	public boolean detect(IConnection connection, IResource resource, IProject eclipseProject) {
		return ProjectUtils.isAccessible(eclipseProject)
				&& "node".equals(new ProjectBuilderTypeDetector().findTemplateFilter(eclipseProject));
	}

}
