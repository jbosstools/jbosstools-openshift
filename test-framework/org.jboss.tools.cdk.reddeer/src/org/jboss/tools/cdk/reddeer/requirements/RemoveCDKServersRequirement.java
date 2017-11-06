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
import java.util.Arrays;
import java.util.List;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.adapter.CDKServerAdapterType;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServersView;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;

/**
 * Requirement for deleting all CDK servers
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
		deleteCDKServers();
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
	
	@Override
	public void runAfter() {
		deleteCDKServers();
	}
	
	private List<Server> getAllServers() {
		CDEServersView view = new CDEServersView();
		view.open();
		
		return view.getServers();
	}
	
	private void deleteCDKServers() {
		for (Server server : getAllServers()) {
			log.info("Found server with name " + server.getLabel().getName());
			if (isCDKServer(server.getTreeItem())) {
				log.info("Deleting server...");
				server.delete(true);
			}
		}
	}
	
	private boolean isCDKServer(TreeItem item) {
		String type = CDKUtils.getServerTypeIdFromItem(item);
		log.info("Server type id is " + type);
		return Arrays.stream(CDKServerAdapterType.values()).anyMatch(e -> e.serverType().equals(type));
	}

}
