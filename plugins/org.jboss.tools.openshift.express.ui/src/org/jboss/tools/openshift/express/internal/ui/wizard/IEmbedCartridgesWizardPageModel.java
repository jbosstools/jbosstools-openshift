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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.net.SocketTimeoutException;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public interface IEmbedCartridgesWizardPageModel {

	public static final String PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES = "selectedEmbeddableCartridges";

	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException, SocketTimeoutException;

	public void selectEmbeddedCartridges(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException;

	public void unselectEmbeddedCartridges(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException;

	public void setSelectedEmbeddableCartridges(Set<IEmbeddableCartridge> selectedEmbeddableCartridges) throws SocketTimeoutException, OpenShiftException;
	
	public boolean isSelected(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException;

	public IDomain getDomain() throws SocketTimeoutException, OpenShiftException;
	
	public IApplication createJenkinsApplication(String name, IProgressMonitor monitor) throws OpenShiftException, SocketTimeoutException;

}