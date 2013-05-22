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
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import java.net.SocketTimeoutException;
import java.util.Set;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;

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

	public boolean isEmbedded(IEmbeddableCartridge cartridge) throws OpenShiftException;

	public IDomain getDomain() throws SocketTimeoutException, OpenShiftException;
}