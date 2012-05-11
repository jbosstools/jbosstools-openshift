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
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public interface IEmbedCartridgesWizardPageModel {

	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges() throws OpenShiftException, SocketTimeoutException;

	public void selectEmbeddedCartridges(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException;

	public void unselectEmbeddedCartridges(IEmbeddableCartridge cartridge) throws OpenShiftException, SocketTimeoutException;

	public boolean hasApplicationOfType(ICartridge cartridge) throws SocketTimeoutException, OpenShiftException;

	public IApplication createJenkinsApplication(String name, IProgressMonitor monitor) throws OpenShiftException;
}