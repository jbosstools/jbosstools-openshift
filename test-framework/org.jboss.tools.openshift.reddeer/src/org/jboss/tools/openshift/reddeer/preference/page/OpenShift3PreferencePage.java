/*******************************************************************************
 * Copyright (c) 2007-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.preference.page;

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.preference.PreferencePage;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * OpenShift preference page has a place to set up path to OpenShift command line tools
 * binary. This 'oc' binary is crucial for many features in OpenShift tools such as
 * port forwarding, build logs, pod logs etc.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class OpenShift3PreferencePage extends PreferencePage {
	
	public OpenShift3PreferencePage(ReferencedComposite composite) {
		super(composite, new String[]{"JBoss Tools", "OpenShift"});
	}
	
	/**
	 * Sets path to oc binary location on local file system. There are supposed to be validators
	 * for text field containing path.
	 *  
	 * @param path path to oc binary
	 */
	public void setOCLocation(String path) {
		getOCLocation().setText(path);
	}
	
	/**
	 * Clear oc binary location.
	 */
	public void clearOCLocation() {
		setOCLocation("");
	}
	
	public LabeledText getOCLocation() {
		return new LabeledText(OpenShiftLabel.TextLabels.OC_LOCATION);
	}
}

