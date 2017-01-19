/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.preference.page;

import org.jboss.reddeer.jface.preference.PreferencePage;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * OpenShift 3 preference page has a place to set up path to OpenShift command line tools
 * binary. This 'oc' binary is crucial for many features in OpenShift tools such as
 * port forwarding, build logs, pod logs etc.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class OpenShift3PreferencePage extends PreferencePage {
	
	public OpenShift3PreferencePage() {
		super("JBoss Tools", "OpenShift 3");
	}
	
	/**
	 * Sets path to oc binary location on local file system. There are supposed to be validators
	 * for text field containing path.
	 *  
	 * @param path path to oc binary
	 */
	public void setOCLocation(String path) {
		new LabeledText(OpenShiftLabel.TextLabels.OC_LOCATION).setText(path);
	}
	
	/**
	 * Clear oc binary location.
	 */
	public void clearOCLocation() {
		setOCLocation("");
	}
}

