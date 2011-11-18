/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.runtime.Assert;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.client.IApplication;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.IEmbeddableCartridge;
import org.jboss.tools.openshift.express.client.IUser;
import org.jboss.tools.openshift.express.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class ApplicationWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_APPLICATION = "application";

	private IUser user;
	private IApplication application;
	private String name;
	private ICartridge cartridge;
	private List<IEmbeddableCartridge> selectedCartridges;

	public ApplicationWizardModel(IUser user) {
		this(null, user);
	}

	public ApplicationWizardModel(IApplication application, IUser user) {
		this.application = application;
		this.user = user;
	}

	public IUser getUser() {
		return user;
	}
	
	public String getName() {
		return name;
	}

	public String setName(String name) {
		return this.name = name;
	}
	
	public void setCartridge(ICartridge cartridge) {
		this.cartridge = cartridge;
	}

	public void createApplication() throws OpenShiftException {
		IApplication application = createApplication(name, cartridge);
		setApplication(application);
	}

	public void setApplication(IApplication application) {
		firePropertyChange(PROPERTY_APPLICATION, this.application, this.application = application);
	}

	public IApplication getApplication() {
		return application;
	}
	
	public IApplication createApplication(String name, ICartridge cartridge) throws OpenShiftException {
		return getUser().createApplication(name, cartridge);
	}
	
	public void setSelectedCartridges(List<IEmbeddableCartridge> selectedCartridges) {
		this.selectedCartridges = selectedCartridges;
	}

	public void embedCartridges() throws OpenShiftException {
		Assert.isNotNull(selectedCartridges);
		List<IEmbeddableCartridge> addedCartridges = new ArrayList<IEmbeddableCartridge>();
		List<IEmbeddableCartridge> removedCartridges = new ArrayList<IEmbeddableCartridge>();
		computeAdditionsAndRemovals(addedCartridges, removedCartridges, selectedCartridges);
		addEmbeddedCartridges(addedCartridges);
		removeEmbeddedCartridges(removedCartridges);
	}

	private void removeEmbeddedCartridges(List<IEmbeddableCartridge> removedCartridges) throws OpenShiftException {
		if (removedCartridges.isEmpty()) {
			return;
		}
		Collections.sort(removedCartridges, new CartridgeComparator());
		application.removeEmbbedCartridges(removedCartridges);
	}

	private void addEmbeddedCartridges(List<IEmbeddableCartridge> addedCartridges) throws OpenShiftException {
		if (addedCartridges.isEmpty()) {
			return;
		}
		Collections.sort(addedCartridges, new CartridgeComparator());
		application.addEmbbedCartridges(addedCartridges);
	}

	private void computeAdditionsAndRemovals(List<IEmbeddableCartridge> addedCartridges,
			List<IEmbeddableCartridge> removedCartridges, List<IEmbeddableCartridge> selectedCartridges)
			throws OpenShiftException {
		ListDiff listDiff = Diffs.computeListDiff(getApplication().getEmbeddedCartridges(), selectedCartridges);
		for (ListDiffEntry entry : listDiff.getDifferences()) {
			if (entry.isAddition()) {
				addedCartridges.add((IEmbeddableCartridge) entry.getElement());
			} else {
				removedCartridges.add((IEmbeddableCartridge) entry.getElement());
			}
		}
	}

	private static class CartridgeComparator implements Comparator<IEmbeddableCartridge> {

		@Override
		public int compare(IEmbeddableCartridge thisCartridge, IEmbeddableCartridge thatCartridge) {
			// mysql has to be added/removed before phpmyadmin
			if (thisCartridge.equals(IEmbeddableCartridge.MYSQL_51)) {
				return -1;
			} else if (thatCartridge.equals(IEmbeddableCartridge.MYSQL_51)) {
				return 1;
			}
			return 0;
		}
	}

}
