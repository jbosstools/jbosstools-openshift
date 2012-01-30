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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;

/**
 * @author André Dietisheim
 */
public class UIUtils {

	public static void selectAllOnFocus(final Text text) {
		text.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				text.selectAll();
			}
		});

	}
	
	/**
	 * Register a {@link ContributionManager}. The contribution manager gets unregistered on control disposal
	 * 
	 * @param id the id
	 * @param contributionManager the contribution manager
	 * @param control the control
	 * 
	 * @see ContributionManager
	 * @see IMenuService
	 * @see DisposeListener
	 */
	public static void registerContributionManager( final String id, final IContributionManager contributionManager,
			final Control control )
	{
		Assert.isNotNull( id );
		Assert.isNotNull( contributionManager );
		Assert.isTrue( control != null && !control.isDisposed()  );

		final IMenuService menuService = ( IMenuService ) PlatformUI.getWorkbench().getService( IMenuService.class );
		menuService.populateContributionManager( ( ContributionManager ) contributionManager, id );
		contributionManager.update( true );
		control.addDisposeListener( new DisposeListener()
		{
			public void widgetDisposed( DisposeEvent e )
			{
				menuService.releaseContributions( ( ContributionManager ) contributionManager );
			}
		} );
	}

	/**
	 * Creates context menu to a given control.
	 * 
	 * @param control the control
	 * 
	 * @return the i menu manager
	 */
	public static IMenuManager createContextMenu( final Control control )
	{
		Assert.isTrue( control != null && !control.isDisposed() );

		MenuManager menuManager = new MenuManager();
		menuManager.add( new GroupMarker( IWorkbenchActionConstants.MB_ADDITIONS ) );

		Menu menu = menuManager.createContextMenu( control );
		control.setMenu( menu );
		return menuManager;
	}
	
}
