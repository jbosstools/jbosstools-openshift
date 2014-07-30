/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.explorer;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.part.PageBook;
import org.jboss.tools.openshift.express.core.IConnectionsModelListener;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectionWizard;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;

/**
 * @author Xavier Coulon
 */
public class OpenShiftExplorerView extends CommonNavigator implements IConnectionsModelListener {

	private static final String CONNECTION_CONTEXT = "org.jboss.tools.openshift.explorer.context.connection";
	private static final String APPLICATION_CONTEXT = "org.jboss.tools.openshift.explorer.context.application";
	private static final String DOMAIN_CONTEXT = "org.jboss.tools.openshift.explorer.context.domain";
	private IContextActivation contextActivation;

	@Override
	protected Object getInitialInput() {
		return ConnectionsModelSingleton.getInstance();
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aparent) {
		CommonViewer viewer = super.createCommonViewer(aparent);
		ConnectionsModelSingleton.getInstance().addListener(this);
		viewer.addSelectionChangedListener(onSelectionChanged());
		return viewer;
	}

	private ISelectionChangedListener onSelectionChanged() {
		return new ISelectionChangedListener(){

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				activateContext(selection);
			}
		};
	}

	private void activateContext(ISelection selection) {
		IContextService contextService = (IContextService) PlatformUI.getWorkbench().getService(IContextService.class);
		if (contextActivation != null) {
			// deactivate previous active context
			contextService.deactivateContext(contextActivation);
		}
		if (UIUtils.isFirstElementOfType(IDomain.class, selection)) {
			this.contextActivation = contextService.activateContext(DOMAIN_CONTEXT);
		} else if (UIUtils.isFirstElementOfType(IApplication.class, selection)) {
			this.contextActivation = contextService.activateContext(APPLICATION_CONTEXT);
		} else if (UIUtils.isFirstElementOfType(Connection.class, selection)) {
			// must be checked after domain, application, adapter may convert any resource to a connection
			this.contextActivation = contextService.activateContext(CONNECTION_CONTEXT);
		}
	}
	
	@Override
	public void dispose() {
		ConnectionsModelSingleton.getInstance().removeListener(this);
		super.dispose();
	}

	public void refreshViewer() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				CommonViewer viewer = getCommonViewer();
				if (DisposeUtils.isDisposed(viewer)) { 
					return;
				}
				viewer.refresh();
				whichControlToShow();
			}
		});
	}

	public void refreshViewer(final Connection connection) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				CommonViewer viewer = getCommonViewer();
				if (DisposeUtils.isDisposed(viewer)) {
					return;
				}
				viewer.refresh(connection);
				whichControlToShow();
			}
		});
	}

	@Override
	public void connectionAdded(Connection connection) {
		refreshViewer();
	}

	@Override
	public void connectionRemoved(Connection connection) {
		refreshViewer();
	}

	@Override
	public void connectionChanged(Connection connection) {
		refreshViewer(connection);
	}
	
	private PageBook pageBook;
	private Control control;
	private Control linkControl;
	
	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		pageBook = new PageBook(parent, SWT.NONE);
		
		super.createPartControl(pageBook);
		
		control = getCommonViewer().getControl();
		linkControl = createLinkControl(toolkit); 
		whichControlToShow();
	}
	
	private Control createLinkControl(FormToolkit kit){
		Form form = kit.createForm(pageBook);
		Composite composite = form.getBody();
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
    
		Link link = new Link(composite, SWT.NONE);
		link.setText(OpenshiftUIMessages.NoConnectionsAreAvailable);
		link.setBackground(pageBook.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridData gd = new GridData(SWT.LEFT, SWT.FILL, true, false);
		link.setLayoutData(gd);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ConnectionWizard wizard = new ConnectionWizard();
				WizardDialog dialog = new WizardDialog(pageBook.getShell(), wizard);
				if( dialog.open() == Window.OK){
					whichControlToShow();
				}
			}
		});
		return form;
	}
	
	private void whichControlToShow(){
		if(ConnectionsModelSingleton.getInstance().getConnections().length < 1){
			pageBook.showPage(linkControl);
		} else{
			pageBook.showPage(control);
		}
	}

}
