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
package org.jboss.tools.openshift.express.internal.ui.console;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.express.internal.core.preferences.OpenShiftPreferences;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IGearGroup;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public class TailFilesWizardPageModel extends ObservableUIPojo {

	private static final String DEFAULT_FILE_PATTERN = "-f -n 100 */logs/*";

	public static final String PROPERTY_FILE_PATTERN = "filePattern";

	public static final String PROPERTY_GEAR_GROUPS = "gearGroups";

	public static final String PROPERTY_SELECTED_GEAR_GROUPS = "selectedGearGroups";
	
	private final IApplication application;

	private String filePattern = DEFAULT_FILE_PATTERN;

	private Collection<IGearGroup> gearGroups;

	private Collection<IGearGroup> selectedGearGroups;

	public TailFilesWizardPageModel(final IApplication app) {
		this.application = app;
		setFilePattern(ensureValidDefault(OpenShiftPreferences.INSTANCE.getTailFileOptions(application)));
	}

	public void setFilePattern(final String filePattern) {
		firePropertyChange(
				PROPERTY_FILE_PATTERN, this.filePattern, this.filePattern = filePattern);
		OpenShiftPreferences.INSTANCE.saveTailFileOptions(application, filePattern);
	}

	public String getFilePattern() {
		return filePattern;
	}

	public void resetFilePattern() {
		setFilePattern(ensureValidDefault(null));
	}

	private String ensureValidDefault(String filePattern) {
		if (StringUtils.isEmpty(filePattern)) {
			return DEFAULT_FILE_PATTERN;
		}
		return filePattern;
	}

	public IApplication getApplication() {
		return application;
	}

	/**
	 * Loads and refresh the {@link IGearGroup} for the current application
	 */
	public void loadGearGroups() {
		setGearGroups(application.getGearGroups());
	}

	/**
	 * @return the gearGroups
	 */
	public Collection<IGearGroup> getGearGroups() {
		return gearGroups;
	}
	
	/**
	 * @param gearGroups the gearGroups to set
	 */
	public void setGearGroups(final Collection<IGearGroup> gearGroups) {
		firePropertyChange(
				PROPERTY_GEAR_GROUPS, this.gearGroups, this.gearGroups = gearGroups);
		// pre-select gear groups that contain the main (Standalone cartridge)
		Set<IGearGroup> selectedGearGroups = new HashSet<IGearGroup>();
		for(IGearGroup gearGroup : gearGroups) {
			for(ICartridge cartridge : gearGroup.getCartridges()) {
				if(cartridge instanceof IStandaloneCartridge) {
					selectedGearGroups.add(gearGroup);
					continue;
				}
			}
		}
		setSelectedGearGroups(selectedGearGroups);
	}

	/**
	 * @return the selected {@link IGearGroup} in the UI.
	 */
	public Collection<IGearGroup> getSelectedGearGroups() {
		return this.selectedGearGroups;
	}
	
	public void setSelectedGearGroups(final Collection<IGearGroup> selectedGearGroups) {
		firePropertyChange(
				PROPERTY_SELECTED_GEAR_GROUPS, this.selectedGearGroups, this.selectedGearGroups = selectedGearGroups);
	}

	public void selectAllGears() {
		setSelectedGearGroups(new HashSet<IGearGroup>(getGearGroups()));
	}

	public void deselectAllGears() {
		setSelectedGearGroups(new HashSet<IGearGroup>());
	}
}
