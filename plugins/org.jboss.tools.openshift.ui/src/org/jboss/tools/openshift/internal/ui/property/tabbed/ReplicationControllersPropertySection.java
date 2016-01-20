/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.internal.ui.property.tabbed;

/**
 * Tabbed property section for displaying Replication Controllers.
 *  
 * @author Fred Bricon
 *
 */
public class ReplicationControllersPropertySection extends OpenShiftResourcePropertySection {

	public ReplicationControllersPropertySection() {
		super("popup:org.jboss.tools.openshift.ui.properties.tab.ReplicationControllersTab");
	}
}
