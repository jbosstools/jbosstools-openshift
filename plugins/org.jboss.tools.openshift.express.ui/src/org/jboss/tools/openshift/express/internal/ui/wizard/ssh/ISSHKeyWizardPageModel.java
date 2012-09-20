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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public interface ISSHKeyWizardPageModel {

	public static final String PROPERTY_NAME = "name";

	public String getName();

	public void setName(String name);

	public boolean hasKeyName(String name);

	public boolean hasPublicKey(String publicKeyContent);

	public void addSSHKey() throws FileNotFoundException, OpenShiftException, IOException;

	public File getPublicKey();
}