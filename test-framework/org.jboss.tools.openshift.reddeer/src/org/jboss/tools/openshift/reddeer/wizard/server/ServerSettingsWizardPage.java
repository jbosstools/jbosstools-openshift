/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.server;

import org.jboss.tools.openshift.reddeer.wizard.importapp.ImportApplicationWizard;

/**
 * @author jnovak@redhat.com
 */
public class ServerSettingsWizardPage {

	public ImportApplicationWizard importProject(){
		return new ImportApplicationWizard().openFromServerAdapterSettings();
	}
}
