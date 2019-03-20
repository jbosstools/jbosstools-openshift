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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.reddeer.junit.internal.runner.ParameterizedRequirementsRunnerFactory;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

/**
 * Downloads latest container runtime version for all CDK/Minishift adapters
 * @author odockal
 *
 */
@UseParametersRunnerFactory(ParameterizedRequirementsRunnerFactory.class)
public class DownloadLatestContainerRuntimeTest extends DownloadContainerRuntimeAbstractTest {

  private CDKVersion version;
  
  public DownloadLatestContainerRuntimeTest(CDKVersion version) {
    this.version = version;
  }
  
  @Parameters(name="{0}")
  public static Collection<CDKVersion> data() {
    return Arrays.asList(CDKVersion.CDK311, CDKVersion.CDK370, CDKVersion.MINISHIFT1320);
  }
  
  @Override
  protected String getServerAdapter() {
    return SERVER_ADAPTER_32;
  }
  
  @Test
  public void testDownloadingLatestRuntime() {
    downloadAndVerifyCDKRuntime(version, USERNAME, PASSWORD);
  }
}
