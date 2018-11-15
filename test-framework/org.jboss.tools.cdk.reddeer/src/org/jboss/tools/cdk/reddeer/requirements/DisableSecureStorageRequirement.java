/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.requirements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithMnemonicTextMatcher;
import org.eclipse.reddeer.eclipse.equinox.security.ui.storage.PasswordProvider;
import org.eclipse.reddeer.eclipse.equinox.security.ui.storage.StoragePreferencePage;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.requirements.DisableSecureStorageRequirement.DisableSecureStorage;

/**
 * Requirement class for switching off all master passwords requests
 * from Secure Storage page in preferences
 * @author odockal
 */
public class DisableSecureStorageRequirement implements Requirement<DisableSecureStorage> {
	
	private static final Logger log = Logger.getLogger(DisableSecureStorageRequirement.class);
	
	@Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DisableSecureStorage {
    }

	@Override
	public void fulfill() {
		setSecureStorageMasterPasswords(false);		
	}

	@Override
	public void setDeclaration(DisableSecureStorage declaration) {
		// no code here
	}

	@Override
	public void cleanUp() {
		setSecureStorageMasterPasswords(true);
	}
	
    private void setSecureStorageMasterPasswords(boolean checked) {
        WorkbenchPreferenceDialog preferenceDialog = new WorkbenchPreferenceDialog();
        StoragePreferencePage storagePage = new StoragePreferencePage(preferenceDialog);

        preferenceDialog.open();
        preferenceDialog.select(storagePage);
        try {
	        new WaitUntil(new WidgetIsFound(
	        		org.eclipse.swt.custom.CLabel.class, 
	        		new WithMnemonicTextMatcher(CDKLabel.Labels.SECURE_STORAGE)),  
	        		TimePeriod.DEFAULT);
	        log.info("Getting master password providers"); 
	        List<PasswordProvider> items = storagePage.getMasterPasswordProviders();
	        for (PasswordProvider item : items) {
	        	log.info("Uncheking table item: " + item.getDescription()); 
	        	item.setEnabled(checked);
	        }
	        new WaitUntil(new JobIsRunning(), TimePeriod.DEFAULT, false);
	        storagePage.apply();
        } catch (WaitTimeoutExpiredException exc) {
        	log.error("Secure Storage preferences page has timed out"); 
        	log.error(exc.getMessage());
        } finally {
	        preferenceDialog.ok();
		}
    }

	@Override
	public DisableSecureStorage getDeclaration() {
		return null;
	}

	@Override
	public long getPriority() {
		return 1000;
	}
	
}

