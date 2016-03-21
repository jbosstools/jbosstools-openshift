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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
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
		if (ConnectionsRegistrySingleton.getInstance().getAll().size() < 1) {
			showPage(explanationsPane);
		} else {
			showPage(connectionsPane);
		}
	}
	
	private void showPage(final Control page) {
		connectionsPane.getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				pageBook.showPage(page);
			}});
	}
	
	/**
	 * Asynchronously refreshes the given {@code element} in the Tree view
	 * @param element the element to refresh, including its label
	 */
	protected void refresh(final Object element) {
		Display.getDefault().asyncExec(() -> {
			if (getCommonViewer().getTree() != null && !getCommonViewer().getTree().isDisposed()) {
				getCommonViewer().refresh(element, true);
			}
		});
	}
	
	private static class OpenShiftExplorerContextsHandler extends Contexts {

		private static final String CONNECTION_CONTEXT = "org.jboss.tools.openshift.explorer.context.connection";
		
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
					activate(CONNECTION_CONTEXT);
				}
			};
		}
	}

	@Override
	public void initListeners(TreeViewer viewer) {
		super.initListeners(viewer);
		Tree tree = viewer.getTree();
		if(tree != null && !tree.isDisposed()) {
			new LinkMouseListener(tree);
		}
	}

	static class LinkMouseListener extends MouseAdapter implements MouseMoveListener {
		Tree tree;

		LinkMouseListener(Tree tree) {
			this.tree = tree;
			tree.addMouseListener(this);
			tree.addMouseMoveListener(this);
		}

		boolean isLink = false;

		@Override
		public void mouseMove(MouseEvent e) {
			if(tree.isDisposed()) {
				return;
			}
			ILink link = getLink(e);
			if(isLink != (link != null)) {
				isLink = (link != null);
				Cursor cursor = isLink ? Display.getDefault().getSystemCursor(SWT.CURSOR_HAND) : null;
				tree.setCursor(cursor);
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			final ILink link = getLink(e);
			if(link != null) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if(tree.isDisposed()) {
							return;
						}
						tree.setCursor(null);
						isLink = false;
						link.execute();
					}
				});
			}
		}

		ILink getLink(MouseEvent e) {
			if(e.getSource() instanceof Tree) {
				Tree tree = (Tree)e.getSource();
				TreeItem t = tree.getItem(new Point(e.x, e.y));
				Object o = t == null ? null : t.getData();
				return o instanceof ILink ? (ILink)o : null;
			}
			return null;
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
