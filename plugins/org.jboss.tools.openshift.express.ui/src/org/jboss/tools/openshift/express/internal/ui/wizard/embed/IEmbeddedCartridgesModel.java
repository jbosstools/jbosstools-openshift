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

import java.util.List;
import java.util.Set;

import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.IApplicationProperties;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author Andre Dietisheim
 */
public interface IEmbeddedCartridgesModel extends IApplicationProperties {

	public IDomain getDomain() throws OpenShiftException;

	public List<IEmbeddableCartridge> getEmbeddableCartridges() throws OpenShiftException;

	public <C extends IEmbeddableCartridge> List<C> getEmbeddedCartridges() throws OpenShiftException;

	public boolean isEmbedded(IEmbeddableCartridge cartridge) throws OpenShiftException;

	public Set<IEmbeddableCartridge> setCheckedEmbeddableCartridges(Set<IEmbeddableCartridge> cartridges);

	public Set<IEmbeddableCartridge> getCheckedEmbeddableCartridges();
	
	public void refresh() throws OpenShiftException;
	
	public Connection getConnection();

}