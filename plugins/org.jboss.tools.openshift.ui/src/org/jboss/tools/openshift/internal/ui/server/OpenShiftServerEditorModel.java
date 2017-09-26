/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.eclipse.wst.server.ui.internal.command.ServerCommand;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 * @author Jeff Maury
 */
public class OpenShiftServerEditorModel extends ServerSettingsWizardPageModel {

	public static final String PROPERTY_OVERRIDE_PROJECT = "overrideProject";

	private boolean overrideProject = false;
	private ServerEditorSection section;
	private boolean initializing = false;

	public OpenShiftServerEditorModel(IServerWorkingCopy server, ServerEditorSection section, Connection connection) {
		super(null, null, null, connection, server, Status.OK_STATUS);
		this.section = section;
	}

	private void update(boolean overrideProject, Connection connection, List<Connection> connections,  
  			org.eclipse.core.resources.IProject deployProject, List<org.eclipse.core.resources.IProject> projects, 
  			String sourcePath, String podPath, boolean isUseInferredPodPath,
  			IResource resource, List<ObservableTreeItem> resourceItems, 
  			IRoute route, boolean isSelectDefaultRoute, Map<IProject, List<IRoute>> routesByProject,
  			boolean useImageDevmodeKey, String devmodeKey, 
			boolean useImageDebugPortKey, String debugPortKey, String debugPortValue) {
  		update(connection, connections, 
  				deployProject, projects, 
  				sourcePath, podPath, isUseInferredPodPath, 
  				resource, resourceItems, 
  				route, isSelectDefaultRoute, routesByProject, 
  				getOCBinaryStatus(),
  				useImageDevmodeKey, devmodeKey, 
  				useImageDebugPortKey, debugPortKey, debugPortValue);
	 	firePropertyChange(PROPERTY_OVERRIDE_PROJECT, this.overrideProject, this.overrideProject = overrideProject);
	}

	public boolean isOverrideProject() {
		return overrideProject;
	}

	public void setOverrideProject(boolean overrideProject) {
		update(overrideProject, getConnection(), getConnections(), getDeployProject(), getProjects(), 
				getSourcePath(), getPodPath(), isUseInferredPodPath(), 
				getResource(), getResourceItems(), 
				getRoute(), isSelectDefaultRoute(), getAllRoutes(),
				isUseImageDevmodeKey(), getDevmodeKey(),
				isUseImageDebugPortKey(), getDebugPortKey(), getDebugPortValue());
	}

	@Override
	protected org.eclipse.core.resources.IProject getProjectOrDefault(org.eclipse.core.resources.IProject project, List<org.eclipse.core.resources.IProject> projects) {
		// don't default to 1st element
		return project;
	}

	@Override
	protected IResource getResourceOrDefault(IResource resource, List<ObservableTreeItem> services) {
		// don't default to 1st element
		return resource;
	}
	
	@Override
	public void setRoute(IRoute newRoute) {
		setRoute(newRoute, !initializing);
	}

	public void setRoute(IRoute newRoute, boolean executeCommand) {
		String prevHost = getHost(getRoute());
		String prevRouteURL = getRouteURL(isSelectDefaultRoute(), getRoute());
		super.setRoute(newRoute);
		String newHost = getHost(newRoute);
		String newRouteURL = getRouteURL(isSelectDefaultRoute(), newRoute);
		// Fire route change
		if( executeCommand ) 
			section.execute(new SetRouteCommand(getServer(), getRoute(), newRoute, newRouteURL, prevHost, newHost));
	}
	
	public class SetRouteCommand extends ServerWorkingCopyPropertyCommand {

		private IRoute oldRoute, newRoute;
		private String oldHost, newHost;
		
		public SetRouteCommand(IServerWorkingCopy server, IRoute oldRoute, IRoute newRoute, String newRouteURL,
				String oldHost, String newHost) {
			super(server, "Set Route...", null, newRouteURL, 
					OpenShiftServerUtils.ATTR_ROUTE, null);
			this.oldRoute = oldRoute;
			this.newRoute = newRoute;
		}

		@Override
		public void undo() {
			super.undo();
			setRoute(oldRoute, false);
			server.setHost(oldHost);
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
			setRoute(newRoute, false);
			IStatus s = super.redo(monitor, adapt);
			server.setHost(newHost);
			return s;
		}
	}

	@Override
	public void setConnection(Connection connection) {
		setConnection(connection, !initializing);
	}

	public void setConnection(Connection connection, boolean executeCommand) {
		Connection previous = getConnection();
		String previousUrl = previous == null ? null : getConnectionUrl(previous);
		String newUrl = connection == null ? null : getConnectionUrl(connection);
		super.setConnection(connection);
		// fire server command 
		//server.setAttribute(OpenShiftServerUtils.ATTR_CONNECTIONURL, connectionUrl);
		if( executeCommand ) 
			section.execute(new SetConnectionCommand(getServer(), previous, connection, newUrl));
	}
	
	public class SetConnectionCommand extends ServerWorkingCopyPropertyCommand {

		private Connection oldConnection, newConnection;

		public SetConnectionCommand(IServerWorkingCopy server, Connection oldConnection, Connection newConnection, String conUrl) {
			super(server, "Set Connection...", null, conUrl, 
					OpenShiftServerUtils.ATTR_CONNECTIONURL, null);
			this.oldConnection = oldConnection;
			this.newConnection = newConnection;
		}

		@Override
		public void undo() {
			super.undo();
			setConnection(oldConnection, false);
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
			setConnection(newConnection, false);
			return super.redo(monitor, adapt);
		}
	}

	@Override
	public void setResource(IResource resource) {
		setResource(resource, !initializing);
	}

	public void setResource(IResource resource, boolean executeCommand) {
		IResource previous = getResource();
		super.setResource(resource);
		// fire server command 
		if (executeCommand)
			section.execute(new SetResourceCommand(getServer(), previous, resource));
	}

	private class SetResourceCommand extends ServerWorkingCopyPropertyCommand {

		private IResource oldResource, newResource;

		public SetResourceCommand(IServerWorkingCopy server, IResource oldResource, IResource newResource) {
			super(server, "Set Resource...", null, OpenShiftResourceUniqueId.get(newResource), 
					OpenShiftServerUtils.ATTR_SERVICE, null);
			this.oldResource = oldResource;
			this.newResource = newResource;
		}

		@Override
		public void undo() {
			super.undo();
			setResource(oldResource, false);
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
			setResource(newResource, false);
			return super.redo(monitor, adapt);
		}
	}
	

	
	@Override 
	public void setDeployProject(org.eclipse.core.resources.IProject project) {
		setDeployProject(project, !initializing);
	}
	
	public void setDeployProject(org.eclipse.core.resources.IProject project, boolean executeCommand) {
//		public static final String PROPERTY_DEPLOYPROJECT = "deployProject";
		org.eclipse.core.resources.IProject previous = super.getDeployProject();
		super.setDeployProject(project);
		// fire server command 
		// 		server.setAttribute(OpenShiftServerUtils.ATTR_DEPLOYPROJECT, deployProjectName);
		if( executeCommand ) 
			section.execute(new SetDeployProjectCommand(getServer(), previous, project));
	}
	

	public class SetDeployProjectCommand extends ServerWorkingCopyPropertyCommand {

		private org.eclipse.core.resources.IProject oldProj, newProj;

		public SetDeployProjectCommand(IServerWorkingCopy server, org.eclipse.core.resources.IProject oldProj, org.eclipse.core.resources.IProject newProj) {
			super(server, "Set Project...", null, ProjectUtils.getName(deployProject), 
					OpenShiftServerUtils.ATTR_DEPLOYPROJECT, null);
			this.oldProj = oldProj;
			this.newProj = newProj;
		}
		
		@Override
		public void undo() {
			super.undo();
			setDeployProject(oldProj, false);
		}
		
		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
			setDeployProject(newProj, false);
			return super.redo(monitor, adapt);
		}
	}
	

	@Override
	public void setSourcePath(String sourcePath) {
		setSourcePath(sourcePath, !initializing);
	}

	public void setSourcePath(String sourcePath, boolean executeCommand) {

		String previous = getSourcePath();
		super.setSourcePath(sourcePath);
		// fire server command 
		if( executeCommand ) 
			section.execute(new SetSourcePathCommand(getServer(), previous, sourcePath));
	}

	public class SetSourcePathCommand extends ServerWorkingCopyPropertyCommand {

		private String oldPath, newPath;

		public SetSourcePathCommand(IServerWorkingCopy server, String oldPath, String newPath) {
			super(server, "Set Source Path...", null, newPath, 
					OpenShiftServerUtils.ATTR_SOURCE_PATH, null);
			this.oldPath = oldPath;
			this.newPath = newPath;
		}

		@Override
		public void undo() {
			super.undo();
			setSourcePath(oldPath, false);
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
			setSourcePath(newPath, false);
			IStatus s = super.redo(monitor, adapt);
			return s;
		}
		
	}
	
	@Override
	public void setPodPath(String podPath) {
		setPodPath(podPath, !initializing);
	}
	
	public void setPodPath(String podPath, boolean executeCommand) {

		String previous = getPodPath();
		super.setPodPath(podPath);
		// fire server command 
		if( executeCommand ) 
			section.execute(new SetPodPathCommand(getServer(), previous, podPath));
	}

	public class SetPodPathCommand extends ServerWorkingCopyPropertyCommand {

		private String oldPath, newPath;

		public SetPodPathCommand(IServerWorkingCopy server, String oldPath, String newPath) {
			super(server, "Set Pod Path...", null, newPath, 
					OpenShiftServerUtils.ATTR_POD_PATH, null);
			this.oldPath = oldPath;
			this.newPath = newPath;
		}

		@Override
		public void undo() {
			super.undo();
			setPodPath(oldPath, false);
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
			setPodPath(newPath, false);
			IStatus s = super.redo(monitor, adapt);
			return s;
		}
	}

	@Override
	public void setUseImageDevmodeKey(boolean useImageDevmodeKey) {
		setUseImageDevmodeKey(useImageDevmodeKey, !initializing);
	}

	public void setUseImageDevmodeKey(boolean useImageDevmodeKey, boolean executeCommand) {
		super.setUseImageDevmodeKey(useImageDevmodeKey);
		if (executeCommand) {
			setDevmodeKey(null, executeCommand);
		}
	}

	@Override
	public void setDevmodeKey(String devmodeKey) {
		setDevmodeKey(devmodeKey, !initializing);
	}

	public void setDevmodeKey(String devmodeKey, boolean executeCommand) {
		String oldDevmodeKey = getDevmodeKey();
		if (executeCommand)
			section.execute(new SetStringCommand(getServer(), "Set devmode key", OpenShiftServerUtils.ATTR_DEVMODE_KEY, 
					oldDevmodeKey, devmodeKey) {

						@Override
						protected void updateModel(String devmodeKey) {
							OpenShiftServerEditorModel.super.setDevmodeKey(devmodeKey);
						}			
			});
	}

	@Override
	public void setUseImageDebugPortKey(boolean useImageDebugPortKey) {
		setUseImageDebugPortKey(useImageDebugPortKey, !initializing);
	}

	public void setUseImageDebugPortKey(boolean useImageDebugPortKey, boolean executeCommand) {
		super.setUseImageDebugPortKey(useImageDebugPortKey);
		if (executeCommand) {
			setDebugPortKey(null, executeCommand);
			setDebugPortValue(null, executeCommand);
		}
	}

	@Override
	public void setDebugPortKey(String debugPortKey) {
		setDebugPortKey(debugPortKey, !initializing);
	}

	public void setDebugPortKey(String debugPortKey, boolean executeCommand) {
		String oldDebugPortKey = getDebugPortKey();
		if (executeCommand)
			section.execute(new SetStringCommand(getServer(), "Set debug port key", OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY, 
					oldDebugPortKey, debugPortKey) {

						@Override
						protected void updateModel(String debugPortKey) {
							OpenShiftServerEditorModel.super.setDebugPortKey(debugPortKey);
						}			
			});
	}

	@Override
	public void setDebugPortValue(String debugPortValue) {
		setDebugPortValue(debugPortValue, !initializing);
	}

	public void setDebugPortValue(String debugPortValue, boolean executeCommand) {
		String oldDebugPortValue = getDebugPortValue();
		if (executeCommand)
			section.execute(new SetStringCommand(getServer(), "Set debug port", OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE, 
					oldDebugPortValue, debugPortValue) {

						@Override
						protected void updateModel(String debugPortValue) {
							OpenShiftServerEditorModel.super.setDebugPortValue(debugPortValue);
						}			
			});
	}

	public abstract class SetStringCommand extends SetValueCommand<String> {

		public SetStringCommand(IServerWorkingCopy server, String cmd, String attributeKey, String oldValue, String newValue) {
			super(server, cmd, attributeKey, oldValue, newValue);
		}

		@Override
		protected String valueToString(String value) {
			return value;
		}
	}

	public abstract class SetValueCommand<T> extends ServerCommand {

		private T oldValue;
		private T newValue;
		private String attributeKey;

		public SetValueCommand(IServerWorkingCopy server, String cmd, String attributeKey, T oldValue, T newValue) {
			super(server, cmd);
			this.attributeKey = attributeKey;
			this.newValue = newValue;
			this.oldValue = oldValue;
		}

		protected abstract void updateModel(T value); 

		protected abstract String valueToString(T value);
	
		@Override
		public void execute() {
			getServer().setAttribute(attributeKey, valueToString(newValue));
			updateModel(newValue);
		}
		
		@Override
		public void undo() {
			updateModel(oldValue);
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
			updateModel(newValue);
			return super.redo(monitor, adapt);
		}
	}
	
	
	public void setInitializing(boolean initializing) {
		this.initializing = initializing;
	}
	
}
