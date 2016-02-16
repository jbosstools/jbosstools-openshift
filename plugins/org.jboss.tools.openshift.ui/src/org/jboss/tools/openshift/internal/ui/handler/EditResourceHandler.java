/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.property.OpenShiftResourceInput;

import com.openshift.restclient.model.IResource;

/**
 * @author Fred Bricon
 */
public class EditResourceHandler extends OpenInWebBrowserHandler {
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		
		IResource resource = UIUtils.getFirstElement(currentSelection, IResource.class);
		Connection connection = null;
		Shell shell = HandlerUtil.getActiveShell(event);
		if ( resource == null) {
			MessageDialog.openWarning(shell, "Nothing to edit", "This is not the resource you are looking for.");
			return null;
		}
		connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
		if (connection != null) {
			IWorkbenchWindow win = HandlerUtil.getActiveWorkbenchWindow(event);
			try {
				openInEditor(win, connection, resource);
			} catch (PartInitException e) {
				String msg = NLS.bind("Error opening {0} : \"{1}\" in editor: {2}", new String[]{resource.getKind(), resource.getName(), e.getMessage()});
				OpenShiftUIActivator.getDefault().getLogger().logError(msg, e);
			}
		}
		return null;
	}

	private void openInEditor(IWorkbenchWindow window, Connection connection, IResource resource) throws PartInitException {
		   IStorageEditorInput input = new OpenShiftResourceInput(connection, resource);
		   IWorkbenchPage page = window.getActivePage();
		   if (page != null) {
			   IEditorRegistry editorRegistry= PlatformUI.getWorkbench().getEditorRegistry();
			   String defaultJsonEditorId = getDefaultJSONEditorDescriptorId(editorRegistry);
			   IEditorDescriptor editorDescriptor = editorRegistry.getDefaultEditor(resource.getName()+".json", null);
			   String editorId = editorDescriptor == null?defaultJsonEditorId:editorDescriptor.getId();
			   try {
				   page.openEditor(input, editorId);
			   } catch (PartInitException O_o) {
				   if (defaultJsonEditorId.equals(editorId)){
					   throw O_o;
				   }
				   //try to fall back on default editor
				   page.openEditor(input, defaultJsonEditorId);
			   }
		   }
	}
	
	private String getDefaultJSONEditorDescriptorId(IEditorRegistry editorRegistry) {
		IEditorDescriptor editorDescriptor = editorRegistry.findEditor("org.eclipse.wst.json.ui.JSONEditor");
		if (editorDescriptor == null) {
			editorDescriptor = editorRegistry.findEditor("org.eclipse.ui.DefaultTextEditor");
		}
		return editorDescriptor.getId();
	}
}