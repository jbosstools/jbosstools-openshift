/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.ui;

import org.jboss.tools.openshift.internal.cdk.server.core.detection.MinishiftVersionLoader.MinishiftVersions;

public class Minishift17LocationSection extends CDK32LocationSection {

	@Override
	protected String isVersionCompatible(MinishiftVersions versions) {
		return Minishift17ServerWizardFragment.isMinishiftVersionCompatible(versions);
	}

}
