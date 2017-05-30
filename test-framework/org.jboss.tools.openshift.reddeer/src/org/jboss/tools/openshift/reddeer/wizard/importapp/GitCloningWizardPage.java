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
package org.jboss.tools.openshift.reddeer.wizard.importapp;

import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.group.DefaultGroup;

/**
 * @author jnovak@redhat.com
 */
public class GitCloningWizardPage {
	
	public void useExistingRepository(boolean useExistingRepository){
		CheckBox useExistingRepositoryCheckBox = new CheckBox(
				new DefaultGroup("Clone destination"), 
				"Do not clone - use existing repository"
		);
		useExistingRepositoryCheckBox.toggle(useExistingRepository);
	}
	
	public boolean projectExists(){
		return new ImportApplicationWizard().getPageDescription()
				.trim().startsWith("There already is a folder named");
	}
	
}
