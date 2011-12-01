 /*******************************************************************************
  * Copyright (c) 2007-2009 Red Hat, Inc.
  * Distributed under license by Red Hat, Inc. All rights reserved.
  * This program is made available under the terms of the
  * Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributor:
  *     Red Hat, Inc. - initial API and implementation
  ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test;

import org.jboss.tools.ui.bot.ext.RequirementAwareSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift SWTBot TestSuite</b>
 * 
 * <br>
 * TestSuite containing following test cases :
 * <ul>
 *  <li>JBDS50_XXXX User credentials validation</li>
 *  <li>JBDS50_XXXX Domain is created, renamed correctly</li>
 * </ul>
 * 
 * @author sbunciak
 */
@SuiteClasses({ 

    CredentialsValidation.class,
    DomainManipulation.class
    
    })
@RunWith(RequirementAwareSuite.class)
public class OpenShiftAllBotTests {

    // TODO : delete test domain created in DomainManipulation
    
}
