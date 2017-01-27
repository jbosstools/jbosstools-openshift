/*******************************************************************************
 * Copyright (c) 2011-2016 Red Hat, Inc.
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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
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
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.jboss.tools.foundation.ui.widget.IWidgetVisitor;
import org.jboss.tools.foundation.ui.widget.WidgetVisitorUtility;

/**
 * @author André Dietisheim
 */
public class UIUtils {
	
	public static final String PACKAGE_EXPLORER_ID = "org.eclipse.jdt.ui.PackageExplorer";
	public static final String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";
	public static final String RESOURCE_NAVIGATOR_ID = "org.eclipse.ui.views.ResourceNavigator";
	
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
			@Override
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
		// assuming it is intentional to not visit the root composite
		new WidgetVisitorUtility(false).accept(composite, visitor);
	}

	public static void enableAllChildren(boolean enabled, Composite composite) {
		// Calling in this fashion to avoid change in behavior. I'm unsure if 
		// it was intentionally coded to avoid visiting root element, or if that was
		// just by chance. 
		new WidgetVisitorUtility(false).setEnablementRecursive(composite, enabled);
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
	
	public static <E> E adapt(Object object, Class<E> clazz) {
		return Adapters.adapt(object, clazz);
	}

	@SuppressWarnings("unchecked")
	public static <E> E[] getElements(ISelection selection, Class<E> clazz) {
			ArrayList<E> elements = new ArrayList<>();
			
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

	/**
	 * Returns the first selected element that's of the given type, in the view identified by the given id.
	 * Returns {@code null} if the view is not opened, doesn't exist or the selected element is not of the requested type.
	 * 
	 * @param viewId the id of the view whose selection we'll look at
	 * @param clazz the type of selected item that you get
	 * @return the selected item of the given type in the given view. 
	 */
	public static <T> T getFirstSelectedElement(String viewId, Class<T> clazz) {
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = workbenchWindow.getSelectionService().getSelection(viewId);
		return getFirstElement(selection, clazz);
	}
	
	/**
	 * Returns the 1st project that's selected in the following views (in a fallback order):
	 * <ul>
	 * <li>package explorer</li>
	 * <li>project explorer</li>
	 * <li>resource navigator</li>
	 * </ul>
	 * @return the project selected in package-, project-explorer or navigator (in this order of prescedence).
	 */
	public static IProject getFirstSelectedWorkbenchProject() {
		IViewPart part = getVisibleProjectsView();
		if (part == null) {
			return null;
		}
		return getFirstSelectedElement(part.getSite().getId(), IProject.class);
	}

	/**
	 * Returns the 1st visible part among the following ones:
	 * <ul>
	 * <li>package explorer</li>
	 * <li>project explorer</li>
	 * <li>resource navigator</li>
	 * </ul>
	 * @return
	 */
	public static IViewPart getVisibleProjectsView() {
		return getFirstVisibleViewPart(PACKAGE_EXPLORER_ID, PROJECT_EXPLORER_ID, RESOURCE_NAVIGATOR_ID);
	}
	
	public static IViewPart getFirstVisibleViewPart(String... partIds) {
		List<IViewPart> parts = getVisibleViewParts(partIds);
		if (parts.isEmpty()) {
			return null;
		}
		return parts.get(0);		
	}

	/**
	 * Returns the visible workbench parts which match the given ids. If no ids
	 * are given all visible parts are returned.
	 * 
	 * @param partIds
	 * @return
	 * 
	 * @see IWorkbenchPart
	 */
	public static List<IViewPart> getVisibleViewParts(String... partIds) {
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = workbenchWindow.getActivePage();
		final List<String> partIdsList = new ArrayList<>();
		List<IViewPart> parts = null;
		if (partIds != null) {
			parts = new ArrayList<>(partIds.length);
		}

		for(IViewReference viewReference : page.getViewReferences()) {
			String partId = viewReference.getId();
			if (partIdsList == null
				|| partIdsList.isEmpty()
				|| partIdsList.contains(partId)) {
				IViewPart part = viewReference.getView(false);
				if (part != null
					&& page.isPartVisible(part)) {
					parts.add(part);
				}
			}
		}

		parts.stream().sorted((part1, part2) -> {
			int indexPart1 = partIdsList.indexOf(part1.getSite().getId());
			int indexPart2 = partIdsList.indexOf(part1.getSite().getId());
			if (indexPart1 > indexPart2) {
				return 1;
			} else {
				return -1;
			}
		});
		
		return parts;
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
	
	/**
	 * Sets the standard width to the given button if it's layouted via
	 * {@code GridLayout}}.
	 * 
	 * @param button
	 */
	public static void setDefaultButtonWidth(Button button) {
		assertCanSetGridData(button);
		((GridData)button.getLayoutData()).widthHint =  
				convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
	}

	/**
	 * Sets equal width to all buttons in the array, which is the maximum
	 * of the standard default button width and the required (computed) button width
	 * for the most wide of the buttons.
	 * Throws IllegalArgumentException if a button is disposed or does not have GridData.
	 * 
	 * @param button
	 */
	public static void setEqualButtonWidth(Button... buttons) {
		if(buttons == null || buttons.length == 0) {
			return;
		}
		int width = convertHorizontalDLUsToPixels(buttons[0], IDialogConstants.BUTTON_WIDTH);
		for (Button button: buttons) {
			assertCanSetGridData(button);
			int w = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			if(width < w) {
				width = w;
			}
		}
		for (Button button: buttons) {
			((GridData)button.getLayoutData()).widthHint = width;
		}
	}

	private static void assertCanSetGridData(Button button) {
		Assert.isLegal(!DisposeUtils.isDisposed(button), "Button is disposed");
		Assert.isLegal(button.getLayoutData() instanceof GridData, "Button " + button.getText() + " is not layouted with a GridLayout");
	}

	private static int convertHorizontalDLUsToPixels(Control control, int dlus) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		int averageWidth= gc.getFontMetrics().getAverageCharWidth();
		gc.dispose();
	
		double horizontalDialogUnitSize = averageWidth * 0.25;
	
		return (int)Math.round(dlus * horizontalDialogUnitSize);
	}
	
	/**
	 * Create a file selection dialog.
	 * 
	 * @param selectedFile the initial selected file. May be null
	 * @param title the title of the dialog
	 * @param message the message of the dialog
	 * @param extension the extension to filter. If null or empty, all files are accepted
	 * @param initialSelection the initial selection resource (used if selectedFile is null)
	 * @return the dialog
	 */
    public static ElementTreeSelectionDialog createFileDialog(String selectedFile,
                                                              String title,
                                                              String message,
                                                              String extension,
                                                              IResource initialSelection) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
                    getShell(),
                    new WorkbenchLabelProvider(),
                    new WorkbenchContentProvider()
        );
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setInput( ResourcesPlugin.getWorkspace().getRoot() );
        dialog.addFilter(new ViewerFilter() {
            
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
				return element instanceof IContainer || 
						(element instanceof IFile && 
								(StringUtils.isEmpty(extension) || extension.equals(((IFile)element).getFileExtension()))
						);
			}
        });
        dialog.setAllowMultiple( false );
        if (StringUtils.isNotBlank(selectedFile)) {
            String prefix = "${workspace_loc:";
            String path = selectedFile;
            if (selectedFile.startsWith(prefix) && selectedFile.endsWith("}")) {
                path = path.substring(prefix.length(), path.length()-1);
            }
            initialSelection = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        }
        if (initialSelection != null) {
            dialog.setInitialSelection(initialSelection);
        }
        
        return dialog;
    }

    /**
     * Use this method instead of HandlerUtil.getCurrentSelection(event)
     * when action is called on Properties View.
     * 
     * @param event
     * @return
     */
    public static ISelection getCurrentSelection(ExecutionEvent event) {
    	IWorkbenchPart part = HandlerUtil.getActivePart(event);
    	IWorkbenchPartSite site = (part != null) ? part.getSite() : null;
    	IWorkbenchWindow window = (site != null) ? site.getWorkbenchWindow() : null;
    	ISelectionService service = (window != null) ? window.getSelectionService() : null;
    	return service != null ? service.getSelection() : null;
    }
    
    /**
     * Makes sure combos in GTK3 are displayed in the correct size.
     * 
     * @param combo the container which contains combos wont have those in too small size
     * 
     * @see https://issues.jboss.org/browse/JBIDE-16877,
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=431425
     */
    public static void ensureGTK3CombosAreCorrectSize(final Composite composite) {
    	if (!Platform.WS_GTK.equals(Platform.getWS())
    			|| DisposeUtils.isDisposed(composite)) {
    		return;
    	}

		composite.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				if (!DisposeUtils.isDisposed(composite)
						&& composite.isVisible()) {
					composite.layout(true, true);
					composite.update();
					composite.removePaintListener(this);
				}
			}
		});
    }
   }
