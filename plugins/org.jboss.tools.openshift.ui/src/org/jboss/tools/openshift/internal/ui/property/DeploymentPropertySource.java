/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.models.Deployment;

import com.openshift.restclient.model.IServicePort;

/**
 * Property source for Deployments
 * 
 * @author jeff.cantrill
 *
 */
public class DeploymentPropertySource implements IPropertySource {

	private Deployment deployment;

	public DeploymentPropertySource(Deployment deployment) {
		this.deployment = deployment;
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return new IPropertyDescriptor[] {
				new ExtTextPropertyDescriptor("service.name", "Name", "Service"),
				new ExtTextPropertyDescriptor("service.port", "Port Mapping", "Service"),
				new ExtTextPropertyDescriptor("service.route", "Route", "Service"),
				new ExtTextPropertyDescriptor("deployment.name", "Name", "Deployment"),
				new ExtTextPropertyDescriptor("deployment.date", "Date", "Deployment"),
				new TextPropertyDescriptor("pods", "Pods") //running, pending, succeeded/ failed/ unknown
		};
	}
	
//	private Collection<IPropertyDescriptor> getPodStatusDescriptors(){
//		Set<IPropertyDescriptor> status = new HashSet<>();
//		for (IPod pod : deployment.getPods()) {
//			status.add(new ExtTextPropertyDescriptor("pod", pod.getStatus(), "Pods"));
//		}
//		return status;
//	}

	@Override
	public Object getPropertyValue(Object id) {
		switch((String)id) {
		case "service.name":
			return deployment.getService().getName();
		case "service.port":
			List<IServicePort> ports = deployment.getService().getPorts();
			if(ports.size() > 0) {
				IServicePort port = ports.get(0);
				return NLS.bind("{0}/{1}->{2}", new Object[] { port.getPort(), port.getProtocol(), port.getTargetPort()});
			}
			break;
		case "service.route":
		case "deployment.name":
//			return NLS.bind("", deployment.get)
		case "deployment.date":
			
		case "pods":
			return deployment.getPods().size();
		}
		return null;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
	}

}
