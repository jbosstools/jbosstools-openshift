/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.core.server.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Messages;
import org.eclipse.wst.server.core.internal.PublishServerJob;
import org.eclipse.wst.server.core.internal.Server;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerBehaviour;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.core.Trace;

public class OpenshiftResourceChangeListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		final IResourceDelta delta = event.getDelta();
		if (delta == null)
			return;
		
		// ignore clean builds
		if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD)
			return;	
		// search for changes to any project using a visitor
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta visitorDelta) {
					IResource resource = visitorDelta.getResource();

					// only respond to project changes
					if (resource != null && resource instanceof IProject) {
						publishHandleProjectChange(visitorDelta, event);
						return false;
					}
					return true;
				}
			});
		} catch (Exception e) {
			Trace.error("Error responding to resource change", e);
		}
	}
	private List<IResource> getDeltaResourceChanges(IResourceDelta delta) {
		final ArrayList<IResource> changed = new ArrayList<>();
		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta delta2) throws CoreException {
				// has this deltaResource been changed?
				if (delta2.getKind() == IResourceDelta.NO_CHANGE)
					return false;
				
				if (delta2.getResource() instanceof IFile) {
					if (delta2.getKind() == IResourceDelta.CHANGED
						&& (delta2.getFlags() & IResourceDelta.CONTENT) == 0
						&& (delta2.getFlags() & IResourceDelta.REPLACED) == 0
						&& (delta2.getFlags() & IResourceDelta.SYNC) == 0){
						// this resource is effectively a no change
						return false;
					}
				}
				// Still here means delta has changes
				changed.add(delta2.getResource());
				return true;
			}
		};
		try {
			delta.accept(visitor);
		} catch(CoreException ce) {
			// TODO log
		}
		return changed;
	}
	
	protected void publishHandleProjectChange(IResourceDelta delta, IResourceChangeEvent event) {
		IProject project = (IProject) delta.getResource();
		if (project == null)
			return;

		List<IResource> changes = getDeltaResourceChanges(delta); 
		if( changes.size() > 0) {
			OpenShiftServer[] servers2 = getPublishRequiredServers(delta);
			int size2 = servers2.length;
			for (int j = 0; j < size2; j++) {
				handleSpecialProjectChange(servers2[j], delta, changes, event);
			}
		}
	}
	
	private OpenShiftServer[] getPublishRequiredServers(IResourceDelta delta){		
		// The list of servers that will require publish
		final List<OpenShiftServer> servers2 = new ArrayList<>();

		// wrksServers = Workspaces Servers
		final IServer[] wrksServers =  ServerCore.getServers();
		for( int i = 0; i < wrksServers.length; i++ ) {
			OpenShiftServer os = (OpenShiftServer)wrksServers[i].loadAdapter(OpenShiftServer.class, new NullProgressMonitor());
			if( os != null ) {
				IProject magic = OpenShiftServerUtils.getDeployProject(wrksServers[i]);
				if( magic != null ) {
					 // Safe because we've already eliminated non-project deltas
					IProject p = (IProject)delta.getResource();
					if( magic.equals(p)) {
						servers2.add(os);
					}
				}
			}
		}
		return (OpenShiftServer[]) servers2.toArray(new OpenShiftServer[servers2.size()]);
	}
	

	protected void handleSpecialProjectChange(OpenShiftServer server, IResourceDelta delta,  List<IResource> changes, IResourceChangeEvent event) {
		// check for duplicate jobs already waiting and don't create a new one
		Job[] jobs = Job.getJobManager().find(ServerUtil.SERVER_JOB_FAMILY);
		if (jobs != null) {
			int size = jobs.length;
			for (int i = 0; i < size; i++) {
				if (jobs[i] instanceof MagicProjectChangeJob) {
					MagicProjectChangeJob rcj = (MagicProjectChangeJob) jobs[i];
					if (rcj.getServer().equals(server.getServer()) && rcj.getState() == Job.WAITING)
						return;
				}
			}
		}
		
		MagicProjectChangeJob job = new MagicProjectChangeJob(server, delta, changes, event);
		job.setSystem(true);
		job.setPriority(Job.BUILD);
		job.schedule();
	}
	
	/**
	 * Give the server an indication that its magic project has been changed somehow
	 */
	public class MagicProjectChangeJob extends Job {
		private IResourceDelta delta;
		private OpenShiftServer openshiftServer;
		private IResourceChangeEvent event;
		private List<IResource> changes;

		public MagicProjectChangeJob(OpenShiftServer openshiftServer, IResourceDelta delta, List<IResource> change, IResourceChangeEvent event) {
			super(NLS.bind(Messages.jobUpdateServer, openshiftServer.getServer().getName()));
			this.openshiftServer = openshiftServer;
			this.delta = delta;
			this.changes = change;
			this.event = event;
			
			ISchedulingRule[] rules = new ISchedulingRule[2];
			IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
			rules[0] = ruleFactory.createRule(delta.getResource());
			rules[1] = openshiftServer.getServer();
			setRule(MultiRule.combine(rules));
		}
		
		public boolean belongsTo(Object family) {
			return ServerUtil.SERVER_JOB_FAMILY.equals(family);
		}
	
		public IServer getServer() {
			return openshiftServer.getServer();
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			IServer server = openshiftServer.getServer();
			OpenShiftServerBehaviour behaviourDelegate = (OpenShiftServerBehaviour)
					server.loadAdapter(OpenShiftServerBehaviour.class, new NullProgressMonitor());

			int oldState = server.getServerPublishState();
			int newState = ((oldState == IServer.PUBLISH_STATE_FULL || oldState == IServer.PUBLISH_STATE_UNKNOWN) 
					? IServer.PUBLISH_STATE_FULL : IServer.PUBLISH_STATE_INCREMENTAL);
			((Server)server).setServerPublishState(newState);
			
			if (server.getServerState() != IServer.STATE_STOPPED && behaviourDelegate != null)
				behaviourDelegate.handleResourceChange();
			
			if (server.getServerState() == IServer.STATE_STARTED)
				autoPublish(event);
			
			return Status.OK_STATUS;
		}
		
		private void autoPublish(IResourceChangeEvent event) {
			boolean buildOccurred = event != null && didBuildOccur(event);
			boolean projectClosedOrDeleted = event != null && isProjectCloseOrDeleteEvent(event);
			
			int auto = ((Server)openshiftServer.getServer()).getAutoPublishSetting();
			if (auto == Server.AUTO_PUBLISH_DISABLE)
				return;
			
			if( (auto == Server.AUTO_PUBLISH_BUILD) && 
					!buildOccurred && !projectClosedOrDeleted)
				return;
			
			int time = ((Server)openshiftServer.getServer()).getAutoPublishTime();
			if (time >= 0) {
				openshiftServer.getServer().publish(IServer.PUBLISH_INCREMENTAL, 
						new NullProgressMonitor());
			}
		}

		private boolean isProjectCloseOrDeleteEvent(IResourceChangeEvent event) {
			int kind = event.getType();
			if( (kind & IResourceChangeEvent.PRE_CLOSE) > 0 || 
					(kind & IResourceChangeEvent.PRE_DELETE) > 0)
				return true;
			return false;
		}
		
		private boolean didBuildOccur(IResourceChangeEvent event) {
			int kind = event.getBuildKind();
			final boolean eventOccurred = 
				   (kind == IncrementalProjectBuilder.INCREMENTAL_BUILD) || 
				   (kind == IncrementalProjectBuilder.FULL_BUILD) || 
				   ((kind == IncrementalProjectBuilder.AUTO_BUILD && 
						ResourcesPlugin.getWorkspace().isAutoBuilding()));
			return eventOccurred;
		}
	}

}
