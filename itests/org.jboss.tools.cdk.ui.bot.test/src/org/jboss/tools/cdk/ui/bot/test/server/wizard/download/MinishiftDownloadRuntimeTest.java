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

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.junit.internal.runner.ParameterizedRequirementsRunnerFactory;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

/**
 * Class covers runtime download test for Minishift 1.14, 1.15.1, 1.16.1, 1.17.0
 * @author odockal
 *
 */
@UseParametersRunnerFactory(ParameterizedRequirementsRunnerFactory.class)
public class MinishiftDownloadRuntimeTest extends DownloadContainerRuntimeAbstractTest {

  private CDKVersion version;
  
  private static final Logger log = Logger.getLogger(MinishiftDownloadRuntimeTest.class);
  
  public MinishiftDownloadRuntimeTest(CDKVersion version) {
    this.version = version;
  }
  
  @Parameters(name="{0}")
  public static Collection<CDKVersion> data() {
    return Arrays.asList(CDKVersion.MINISHIFT1140, CDKVersion.MINISHIFT1151, CDKVersion.MINISHIFT1161, CDKVersion.MINISHIFT1170);
  }
  
  @Override
  protected String getServerAdapter() {
    return SERVER_ADAPTER_MINISHIFT;
  }
  
  @Test
  public void testDownloadingMinishiftRuntime() {
    try {
      downloadAndVerifyCDKRuntime(version, "", "");
    } catch (AssertionError err) {
      if (err.getMessage().contains(CDKLabel.Messages.DOES_NOT_EXIST) && CDKUtils.IS_WINDOWS) {
        log.error("Skipped due to JBIDE-26014 on windows");
      } else {
        throw err;
      }
    }
  }
	
}
