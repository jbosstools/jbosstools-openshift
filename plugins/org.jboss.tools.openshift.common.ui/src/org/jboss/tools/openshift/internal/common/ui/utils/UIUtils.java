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
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;

/**
 * @author André Dietisheim
 */
public class UIUtils {
	
	private static final String OPENSHIFT_EXPLORER_VIEW_ID = "org.jboss.tools.openshift.express.ui.explorer.expressConsoleView";

	public static void showOpenShiftExplorerView() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.showView(OPENSHIFT_EXPLORER_VIEW_ID);
				} catch (PartInitException e) {
					OpenShiftCommonUIActivator.getDefault().getLogger().logError("Failed to show the OpenShift Explorer view", e);
				}
			}
		});
	}
	
	public static void selectAllOnFocus(final Text text) {
		final FocusListener onFocus = new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				text.selectAll();
			}
		};
		text.addFocusListener(onFocus);
		text.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				text.removeFocusListener(onFocus);
			}
		});
	}

	/**
	 * Ensures that the given text gets the focus if the given button is
	 * selected.
	 * 
	 * @param button
	 * @param text
	 */
	public static void focusOnSelection(final Button button, final Text text) {
		final Listener onSelect = new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (!button.getSelection()) {
					// button was deselected, got selected
					text.selectAll();
					text.setFocus();
				}
			}
		};
		button.addListener(SWT.Selection, onSelect);
		button.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				button.removeListener(SWT.Selection, onSelect);
			}
		});
	}

	/**
	 * Register a {@link ContributionManager}. The contribution manager gets
	 * unregistered on control disposal
	 * 
	 * @param id
	 *            the id
	 * @param contributionManager
	 *            the contribution manager
	 * @param control
	 *            the control
	 * 
	 * @see ContributionManager
	 * @see IMenuService
	 * @see DisposeListener
	 */
	public static void registerContributionManager(final String id, final IContributionManager contributionManager,
			final Control control)
	{
		Assert.isNotNull(id);
		Assert.isNotNull(contributionManager);
		Assert.isTrue(control != null && !control.isDisposed());

		final IMenuService menuService = (IMenuService) PlatformUI.getWorkbench().getService(IMenuService.class);
		menuService.populateContributionManager((ContributionManager) contributionManager, id);
		contributionManager.update(true);
		control.addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent e)
			{
				menuService.releaseContributions((ContributionManager) contributionManager);
			}
		});
	}

	/**
	 * Creates context menu to a given control.
	 * 
	 * @param control
	 *            the control
	 * 
	 * @return the i menu manager
	 */
	public static IMenuManager createContextMenu(final Control control)
	{
		Assert.isTrue(control != null && !control.isDisposed());

		MenuManager menuManager = new MenuManager();
		menuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		Menu menu = menuManager.createContextMenu(control);
		control.setMenu(menu);
		return menuManager;
	}

	public static void doForAllChildren(IWidgetVisitor visitor, Composite composite) {
		if (composite == null
				|| composite.isDisposed()) {
			return;
		}
		for (Control control : composite.getChildren()) {
			if (control instanceof Composite) {
				doForAllChildren(visitor, (Composite) control);
			}
			visitor.visit(control);
		}
	}

	public static interface IWidgetVisitor {
		public void visit(Control control);
	}
	
	public static void enableAllChildren(boolean enabled, Composite composite) {
		doForAllChildren(new IWidgetVisitor() {
			
			@Override
			public void visit(Control control) {
				control.setEnabled(enabled);
			}
		}, composite);
	}
	
	public static Shell getShell() {
		Shell shell = null;
		final IWorkbenchWindow window =
				PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			shell = window.getShell();
		}
		return shell;
	}

	public static boolean isFirstElementOfType(Class<?> clazz, ISelection selection) {
		return getFirstElement(selection, clazz) != null;
	}
	
	public static <E> E getFirstElement(ISelection selection, Class<E> clazz) {
			return adapt(getFirstElement(selection), clazz);
	}
	
	@SuppressWarnings("unchecked")
	public static <E> E adapt(Object object, Class<E> clazz) {
		return Adapters.adapt(object, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <E> E[] getElements(ISelection selection, Class<E> clazz) {
			ArrayList<E> elements = new ArrayList<E>();
			
			for(Object element: getElements(selection)) {
				E adapted = Adapters.adapt(element, clazz);
				if( adapted != null )
					elements.add(adapted);
			}
			return elements.toArray((E[])Array.newInstance(clazz, 0));
	}
	
	public static Object getFirstElement(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}
		return ((IStructuredSelection) selection).getFirstElement();
	}

	public static Object[] getElements(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return new Object[0];
		}
		return ((IStructuredSelection) selection).toArray();
	}

	public static boolean areNumOfElementsSelected(int numOf, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return false;
		}
		
		return ((IStructuredSelection) selection).size() == numOf;
	}

	public static boolean isSingleSelection(ISelection selection) {
		return areNumOfElementsSelected(1, selection);
	}
	
	public static void copyBackground(Control source, Control destination) {
		destination.setBackground(source.getBackground());
	}

	public static void copyMenuOf(Control source, final Control target) {
		Menu menu = source.getMenu();
		if (menu != null) {
			target.setMenu(menu);
		}
	}

	public static void ensureDisplayExec(Runnable runnable) {
		Display display = Display.getCurrent();
		if (display == null) {
			Display.getDefault().asyncExec(runnable);
		} else {
			runnable.run();
		}
	}
	
	public static Text createSearchText(Composite parent) {
		final Text searchText = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH);
		searchText.setMessage("type filter text");
		searchText.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (e.detail == SWT.CANCEL) {
					searchText.setText("");
				}
			}
		});
		return searchText;
	}

	/**
	 * Sets the visibility of the given control, its {@link GridData#exclude} and relayouts its parent.
	 * 
	 * @param visible
	 * @param control
	 */
	public static void setVisibleAndExclude(boolean visible, final Control control) {
		Assert.isLegal(control != null && !control.isDisposed());
		Assert.isLegal(control.getLayoutData() instanceof GridData);
		
		control.setVisible(visible);
		((GridData) control.getLayoutData()).exclude = !visible;
		control.getParent().layout();
	}

	public static <T> ISelection createSelection(T selectedObject) {
		ISelection selection = new StructuredSelection();
		if (selectedObject != null) {
			selection = new StructuredSelection(selectedObject);
		}
		return selection;
	}
}
