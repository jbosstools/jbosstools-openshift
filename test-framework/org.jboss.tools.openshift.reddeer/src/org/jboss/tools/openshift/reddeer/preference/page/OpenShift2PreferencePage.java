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
 * OpenShift 2 preference page is meant to be handler for remote request time out of OpenShift 2 applications.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class OpenShift2PreferencePage extends PreferencePage {

	/**
	 * Creates a new OpenShift 2 preference page.
	 */
	public OpenShift2PreferencePage() {
		super("JBoss Tools", "OpenShift 2");
	}
	
	/**
	 * Sets remote request time out field to specified string value. Beware, there could be
	 * a validation mark because not all values are allowed.
	 * 
	 * @param value string value of time out
	 */
	public void setRemoteRequestTimeout(String value) {
		new LabeledText(OpenShiftLabel.TextLabels.REMOTE_REQUEST_TIMEOUT).setText(value);
	}
	
	/**
	 * Types remote request time char by char. Beware, there could be
	 * a validation mark because not all values are allowed.
	 *  
	 * @param value string value of time out
	 */
	public void typeRemoteRequestTimeout(String value) {
		new LabeledText(OpenShiftLabel.TextLabels.REMOTE_REQUEST_TIMEOUT).typeText(value);
	}
	
	/**
	 * Sets remote request time out to default value and that is 0. 
	 */
	public void clearRemoteRequestTimeout() {
		setRemoteRequestTimeout("0");
	}
	
	/**
	 * Gets current value of remote request time out.
	 * 
	 * @return string of current remote request time out
	 */
	public String getRemoteRequestTimeout() {
		return new LabeledText(OpenShiftLabel.TextLabels.REMOTE_REQUEST_TIMEOUT).getText(); 
	}
}
