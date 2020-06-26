/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.view.resources;

import org.eclipse.reddeer.swt.api.TreeItem;

/**
 * 
 * Abstract OpenShift Application Explorer Connection implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public abstract class AbstractOpenShiftApplicationConnection extends AbstractOpenShiftApplicationExplorerItem {

	public AbstractOpenShiftApplicationConnection(TreeItem connectionItem) {
		super(connectionItem);
	}
	
}
