package org.jboss.tools.openshift.core.server.adapter;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.common.core.connection.IConnection;

import com.openshift.restclient.model.IResource;

public interface IOpenshiftServerAdapterProfileDetector {

	String getProfile();
	boolean detect(IConnection connection, IResource resource, IProject eclipseProject);
}