/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models.applicationexplorer;

import java.io.IOException;

import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIModel;
import org.jboss.tools.openshift.internal.ui.odo.OdoCli;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * @author Red Hat Developers
 *
 */
public class ApplicationExplorerUIModel extends AbstractOpenshiftUIModel<ApplicationExplorerUIModel.ClusterInfo, ApplicationExplorerUIModel> {

	private static ApplicationExplorerUIModel INSTANCE;
	
	public static ApplicationExplorerUIModel getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ApplicationExplorerUIModel();
		}
		return INSTANCE;
	}
	
	static class ClusterInfo {
		private OpenShiftClient client = loadClient();
		
		public Odo getOdo() throws IOException {
			return OdoCli.get();
		}

		/**
		 * @return
		 */
		public OpenShiftClient getClient() {
			return client;
		}
		
		private OpenShiftClient loadClient() {
		    return new DefaultOpenShiftClient(new ConfigBuilder().build());
		}

	}
	
	private ApplicationExplorerUIModel() {
		super(null, new ClusterInfo());
	}

	@Override
	public void refresh() {
		fireChanged(this);
	}
	
	public Odo getOdo() throws IOException {
		return getWrapped().getOdo();
	}
	
	public OpenShiftClient getClient() {
		return getWrapped().getClient();
	}

}
