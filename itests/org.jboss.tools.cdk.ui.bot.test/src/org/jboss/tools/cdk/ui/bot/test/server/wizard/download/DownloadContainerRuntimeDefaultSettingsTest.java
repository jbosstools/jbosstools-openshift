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
package org.jboss.tools.cdk.ui.bot.test.server.wizard.download;

import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.junit.Test;

/**
 * Class covers runtime download test for defaults of CDK 3.2+ and Minishift 1.14
 * @author odockal
 *
 */
public class DownloadContainerRuntimeDefaultSettingsTest extends DownloadContainerRuntimeAbstractTest {

	@Override
	protected String getServerAdapter() {
		return SERVER_ADAPTER_32;
	}
	
	@Test
	public void testDownloadingCDK340RuntimeDefaults() {
		downloadAndVerifyContainerRuntime(CDKVersion.CDK340, USERNAME, PASSWORD, "", "", true, true);
	}
	
	@Test
	public void testDownloadingMinishiftRuntimeDefaults() {
		downloadAndVerifyContainerRuntime(CDKVersion.MINISHIFT1170, "", "", "", "", true, true);
	}
	
}
