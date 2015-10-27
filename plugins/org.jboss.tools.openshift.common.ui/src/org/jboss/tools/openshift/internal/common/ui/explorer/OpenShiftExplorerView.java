/*******************************************************************************
 * Copyright (c) 2011-2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.explorer;


import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class OpenShiftExplorerView extends CommonNavigator implements IConnectionsRegistryListener {
	
	private Control connectionsPane;
	private Control explanationsPane;
	private PageBook pageBook;

	@Override
	protected Object getInitialInput() {
		return ConnectionsRegistrySingleton.getInstance();
	}
	
	@Override
	protected CommonViewer createCommonViewer(Composite parent) {
		CommonViewer viewer = super.createCommonViewer(parent);
		new OpenShiftExplorerContextsHandler(viewer);
		return viewer;
	}

	@Override
	public void dispose() {
		ConnectionsRegistrySingleton.getInstance().removeListener(this);
		super.dispose();
	}

	@Override
	public void connectionAdded(IConnection connection) {
		showConnectionsOrExplanations();
	}
	
	private void showConnectionsOrExplanations() {
		asyncShowConnectionsOrExplanations();
	}
	
	private void asyncShowConnectionsOrExplanations() {
		Display.getDefault().asyncExec(new Runnable() { 
			public void run() {
				showConnectionsOrExplanations(connectionsPane, explanationsPane);
			}
		});
	}

	@Override
	public void connectionRemoved(IConnection connection) {
		asyncShowConnectionsOrExplanations();
	}

	@Override
	public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		this.pageBook = new PageBook(parent, SWT.NONE);

		super.createPartControl(pageBook);

		this.connectionsPane = getCommonViewer().getControl();
		this.explanationsPane = createExplanationPane(connectionsPane, pageBook, toolkit);
		showConnectionsOrExplanations(connectionsPane, explanationsPane);
		ConnectionsRegistrySingleton.getInstance().addListener(this);
	}

	private Control createExplanationPane(Control connectionsPane, PageBook pageBook, FormToolkit kit) {
		Form form = kit.createForm(pageBook);
		Composite composite = form.getBody();
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);

		Link link = new Link(composite, SWT.NONE);
		link.setText("No connections are available. Create a new connection with the <a>New Connection Wizard...</a>");
		link.setBackground(pageBook.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL).grab(true, false).applyTo(link);
		link.addSelectionListener(onExplanationClicked(connectionsPane, link));
		return form;
	}

	private SelectionAdapter onExplanationClicked(final Control connectionsPane, final Control explanationPane) {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ConnectionWizard wizard = new ConnectionWizard();
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
						.getShell(), wizard);
				if (dialog.open() == Window.OK) {
					showConnectionsOrExplanations(connectionsPane, explanationPane);
				}
			}
		};
	}

	private void showConnectionsOrExplanations(Control connectionsPane, Control explanationsPane) {
		if (ConnectionsRegistrySingleton.getInstance().getAll().isEmpty()) {
			pageBook.showPage(explanationsPane);
		} else {
			pageBook.showPage(connectionsPane);
		}
	}
	
	private static class OpenShiftExplorerContextsHandler extends Contexts {

		private static final String CONNECTION_CONTEXT = "org.jboss.tools.openshift.explorer.context.connection";
//		private static final String APPLICATION_CONTEXT = "org.jboss.tools.openshift.explorer.context.application";
//		private static final String DOMAIN_CONTEXT = "org.jboss.tools.openshift.explorer.context.domain";
		
		OpenShiftExplorerContextsHandler(CommonViewer viewer) {
			viewer.getControl().addFocusListener(onFocusLost());
			viewer.addSelectionChangedListener(onSelectionChanged());
		}
		
		private FocusAdapter onFocusLost() {
			return new FocusAdapter() {
				
				@Override
				public void focusLost(FocusEvent event) {
					deactivateCurrent();
				}
			};
		}

		private ISelectionChangedListener onSelectionChanged() {
			return new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					ISelection selection = event.getSelection();
//					if (UIUtils.isFirstElementOfType(IDomain.class, selection)) {
//						activate(DOMAIN_CONTEXT);
//					} else if (UIUtils.isFirstElementOfType(IApplication.class, selection)) {
//						activate(APPLICATION_CONTEXT);
//					} else if (UIUtils.isFirstElementOfType(ExpressConnection.class, selection)) {
//						// must be checked after domain, application, adapter may convert
//						// any resource to a connection
						activate(CONNECTION_CONTEXT);
//					}
				}
			};
		}
	}
	
	private static class Contexts {
		
		private IContextActivation contextActivation;
		
		public void activate(String contextId) {
			deactivateCurrent();
			IContextService service = getService();
			this.contextActivation = service.activateContext(contextId);
		}
		
		public void deactivateCurrent() {
			if (contextActivation != null) {
				IContextService service = getService();
				service.deactivateContext(contextActivation);
			}
		}
		
		private IContextService getService() {
			return (IContextService) PlatformUI.getWorkbench().getService(IContextService.class);
		}
	}


}
