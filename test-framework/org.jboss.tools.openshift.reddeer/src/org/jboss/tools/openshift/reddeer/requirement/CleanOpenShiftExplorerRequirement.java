/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;

public class CleanOpenShiftExplorerRequirement implements Requirement<CleanOpenShiftExplorer> {

	private CleanOpenShiftExplorer cleanOpenShiftExplorer;
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface CleanOpenShiftExplorer {
		String connectionURL() default StringUtils.EMPTY;
	}

	@Override
	public void fulfill() {
		ConnectionsRegistrySingleton.getInstance().clear();
	}

	public void setDeclaration(CleanOpenShiftExplorer cleanOpenshiftExplorer) {
		this.cleanOpenShiftExplorer = cleanOpenshiftExplorer;
		
	}

	@Override
	public void cleanUp() {
		// NOTHING TO DO
	}

	public CleanOpenShiftExplorer getDeclaration() {
		// TODO Auto-generated method stub
		return this.cleanOpenShiftExplorer;
	}


}
