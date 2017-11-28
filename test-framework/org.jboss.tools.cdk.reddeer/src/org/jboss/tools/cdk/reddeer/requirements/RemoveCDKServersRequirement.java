/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
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

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;

/**
 * Requirement for deleting all CDK servers
 * 
 * @author odockal
 *
 */
public class RemoveCDKServersRequirement implements Requirement<RemoveCDKServers> {

	private static final Logger log = Logger.getLogger(RemoveCDKServersRequirement.class);

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RemoveCDKServers {
	}

	@Override
	public void fulfill() {
		log.info("Deleting all CDK server adapters...");
		CDKUtils.deleteAllCDKServerAdapters();
	}

	@Override
	public void setDeclaration(RemoveCDKServers declaration) {
		// no action
	}

	@Override
	public RemoveCDKServers getDeclaration() {
		return null;
	}

	@Override
	public void cleanUp() {
		// already happens in runAfter
	}
	
	/**
	 * https://github.com/eclipse/reddeer/issues/1847
	 */
	@Override
	public void runAfter() {
		// nothing
	}

}
