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
package org.jboss.tools.cdk.ui.bot.test.server.runtime;

import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.server.ServerAdapter;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement;
import org.jboss.tools.cdk.reddeer.requirements.ContainerRuntimeServerRequirement.ContainerRuntimeServer;
import org.jboss.tools.cdk.reddeer.requirements.DisableSecureStorageRequirement.DisableSecureStorage;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;

@DisableSecureStorage
@RemoveCDKServers
@ContainerRuntimeServer(
		version = CDKVersion.CDK3160,
		usernameProperty="developers.username",
		passwordProperty="developers.password",
		createServerAdapter=false,
		useExistingBinaryInProperty="cdk32.minishift")
public class CDK32RuntimeDetectionTest extends CDKRuntimeDetectionTemplate {

	@InjectRequirement
	private static ContainerRuntimeServerRequirement serverRequirement;
	
	@Override
	public ServerAdapter getServerAdapter() {
		return serverRequirement.getServerAdapter();
	}
}
