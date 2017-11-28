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

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.preference.PreferencePage;
import org.eclipse.reddeer.swt.impl.button.PushButton;

/**
 * OpenShift.io preference page its used for showing information about OpenShift.io account.
 * @author jkopriva@redhat.com
 */
public class OpenShifIOPreferencePage extends PreferencePage {

	public OpenShifIOPreferencePage(ReferencedComposite composite) {
			super(composite, "JBoss Tools", "OpenShift.io");
		}
	
	/**
	 * Remove OpenShift.io account.
	 */
	public OpenShifIOPreferencePage remove() {
		new PushButton(this, "Remove").click();
		return this;
	}
	
	public boolean existsOpenShiftIOAccount() {
		return new PushButton(this, "Remove").isEnabled();
	}

}
