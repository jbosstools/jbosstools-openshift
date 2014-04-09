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

import java.util.Set;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public interface IEmbedCartridgesWizardPageModel {

	public static final String PROPERTY_EMBEDDABLE_CARTRIDGES = "embeddableCartridges";
	public static final String PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGE = "selectedEmbeddableCartridge";
	public static final String PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES = "checkedEmbeddableCartridges";

	public void setCheckedEmbeddableCartridges(Set<ICartridge> selectedEmbeddableCartridges) throws OpenShiftException;

	public Set<ICartridge> getCheckedEmbeddableCartridges() throws OpenShiftException;

	public void checkEmbeddedCartridge(ICartridge cartridge) throws OpenShiftException;

	public void uncheckEmbeddedCartridge(ICartridge cartridge) throws OpenShiftException;
	
	public void setSelectedEmbeddableCartridge(ICartridge cartridge);
	
	public ICartridge getSelectedEmbeddableCartridge();
	
	public boolean isEmbedded(ICartridge cartridge) throws OpenShiftException;

	public IDomain getDomain() throws OpenShiftException;

}