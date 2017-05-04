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
import org.jboss.reddeer.swt.impl.button.CheckBox;


/**
 * Just a stub representing JavaDebugPreferencePage
 * @author rhopp
 *
 */
public class JavaDebugPreferencePage extends PreferencePage{
	
	public JavaDebugPreferencePage() {
		super("Java", "Debug");
	}

	public void setSuspendOnUncaughtExceptions(boolean checked){
		new CheckBox("Suspend execution on uncaught exceptions").toggle(checked);
	}
	
}
