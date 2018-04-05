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
package org.jboss.tools.cdk.reddeer.requirements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.linuxtools.docker.reddeer.ui.DockerExplorerView;
import org.eclipse.linuxtools.docker.reddeer.ui.resources.DockerConnection;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.jboss.tools.cdk.reddeer.requirements.CleanDockerExplorerRequirement.CleanDockerExplorer;

/**
 * Requirement assuring that all Docker connections are removed from Docker Explorer.
 * @author odockal
 *
 */
public class CleanDockerExplorerRequirement implements Requirement<CleanDockerExplorer> {

	private CleanDockerExplorer cleanDocker;
	
	private static final Logger log = Logger.getLogger(CleanDockerExplorerRequirement.class);
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface CleanDockerExplorer {
		/**
		 * Decides if to remove all connections when {@link #cleanup()} is called.
		 * @return boolean, default is true
		 */
		boolean cleanup() default false;
	}

	@Override
	public void fulfill() {
		removeAllDockerConnections();
	}

	@Override
	public void setDeclaration(CleanDockerExplorer declaration) {
		this.cleanDocker = declaration;
	}

	@Override
	public CleanDockerExplorer getDeclaration() {
		return this.cleanDocker;
	}

	@Override
	public void cleanUp() {
		if (this.cleanDocker.cleanup()) {
			removeAllDockerConnections();
		}
	}
	
	public void initializeExplorer() {
		new DockerExplorerView().open();
	}
	
	public List<DockerConnection> getDockerConnections() {
		initializeExplorer();
		try {
			return new DefaultTree().getItems().stream()
					.map(x -> new DockerConnection(x))
					.collect(Collectors.toList()); 
		} catch (CoreLayerException coreExc) {
			// there is no item in docker explorer
		}
		return new ArrayList<>();
	}
	
	public void removeAllDockerConnections() {
		log.info("Getting all available Docker connections..."); 
		List<DockerConnection> connections = getDockerConnections();
		if (!connections.isEmpty()) {
			connections.stream()
			.forEach(x -> {
				log.info("Removing: " + x.getName()); 
				x.removeConnection();
			});
		} else {
			log.info("There was no connection..."); 
		}		
	}

}
