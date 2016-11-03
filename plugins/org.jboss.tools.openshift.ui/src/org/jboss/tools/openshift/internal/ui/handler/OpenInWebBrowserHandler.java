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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.SelectRouteDialog;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class OpenInWebBrowserHandler extends AbstractHandler {
	private static final String NO_ROUTE_MSG = "Could not find a route that points to an url to show in a browser.";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Shell shell = HandlerUtil.getActiveShell(event);

		ISelection currentSelection = UIUtils.getCurrentSelection(event);
		final IRoute route = UIUtils.getFirstElement(currentSelection, IRoute.class);

		//Open route
		if (route != null) {
			return openBrowser(shell, route);
		}
		IServiceWrapper service = UIUtils.getFirstElement(currentSelection, IServiceWrapper.class);
		if (service != null) {
			new RouteOpenerJob(service.getWrapped().getNamespace(), shell) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					this.routes = service.getResourcesOfKind(ResourceKind.ROUTE).stream().map(r -> (IRoute)r.getWrapped()).collect(Collectors.toList());
					return Status.OK_STATUS;
				}
			}.schedule();
			return Status.OK_STATUS;
		}
		
		//Open Project
		final IProject project = UIUtils.getFirstElement(currentSelection, IProject.class);
		if (project != null) {
			new RouteOpenerJob(project.getName(), shell) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					this.routes = project.getResources(ResourceKind.ROUTE);
					return Status.OK_STATUS;
				}
			}.schedule();
			return Status.OK_STATUS;
		}
		
		//Open Connection
		final IConnection connection = UIUtils.getFirstElement(currentSelection, IConnection.class);
		if (connection != null) {
			return openBrowser(shell, connection.getHost());
		}

		return nothingToOpenDialog(shell);
	}
	
	static IStatus nothingToOpenDialog(Shell shell) {
		MessageDialog.openWarning(shell,"No Route", NO_ROUTE_MSG);
		return OpenShiftUIActivator.statusFactory().cancelStatus(NO_ROUTE_MSG);
	}
	
	protected IStatus openBrowser(Shell shell, IRoute route) {
		if (route == null) {
			return nothingToOpenDialog(shell);
		}
		return openBrowser(shell, route.getURL());
	}

	protected IStatus openBrowser(Shell shell, String url) {
		if (StringUtils.isBlank(url)) {
			return nothingToOpenDialog(shell);
		}
		new BrowserUtility().checkedCreateInternalBrowser(url,
				"", OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
		return Status.OK_STATUS;
	}

	private abstract class RouteOpenerJob extends UIUpdatingJob {
		protected List<IRoute> routes;
		private Shell shell;

		public RouteOpenerJob(String projectName, Shell shell) {
			super(NLS.bind("Loading routes for project {0}", projectName));
			this.shell = shell;
		}

		@Override
		protected IStatus updateUI(IProgressMonitor monitor) {
			if (routes == null || routes.isEmpty()) {
				return nothingToOpenDialog(shell);
			}

			if (routes.size() == 1) {
				return openBrowser(shell, routes.get(0));
			}
			IRoute selectedRoute = SelectedRoutePreference.instance.getSelectedRoute(routes);
			if(selectedRoute != null) {
				openBrowser(shell, selectedRoute);
				return Status.OK_STATUS;
			}

			SelectRouteDialog routeDialog = new SelectRouteDialog(routes, shell);
			if (routeDialog.open() == Dialog.OK) {
				selectedRoute = routeDialog.getSelectedRoute();
				if(routeDialog.isRememberChoice()) {
					SelectedRoutePreference.instance.setSelectedRoute(routes, selectedRoute);
				}
				return openBrowser(shell, selectedRoute);
			}
			return Status.OK_STATUS;
		}

	}

}

class SelectedRoutePreference {
	public static final String SELECTED_ROUTE = "org.jboss.tools.openshift.selectedRoute";
	public static final SelectedRoutePreference instance = new SelectedRoutePreference();

	//Since requested choices are moved to the most recent there is no need in long history. 
	static final int HISTORY_LIMIT = 100;
	static final String ITEMS_SEPARATOR = ";", ITEM_SEPARATOR = "|", KEY_SEPARATOR = ",";

	IPreferenceStore preferences = OpenShiftUIActivator.getDefault().getCorePreferenceStore();

	//Maps key for list of routes to key of route
	Map<String, String> choices = new HashMap<>();
	List<String> history = new ArrayList<String>();

	public SelectedRoutePreference() {
		load(preferences.getString(SELECTED_ROUTE));
	}

	private void load(String storedValue) {
		if(StringUtils.isBlank(storedValue)) return;
		String[] items = storedValue.split(ITEMS_SEPARATOR);
		for (String item: items) {
			if(StringUtils.isBlank(item)) continue;
			String[] keys = item.split(ITEM_SEPARATOR);
			if(keys.length == 2) {
				history.add(keys[0]);
				choices.put(keys[0], keys[1]);
			}
		}
	}

	public IRoute getSelectedRoute(List<IRoute> routes) {
		String key = getKey(routes);
		if(choices.containsKey(key)) {
			String selectedRoute = choices.get(key);
			for (IRoute route: routes) {
				if(selectedRoute.equals(getKey(route))) {
					//move item to most recent
					history.remove(key);
					history.add(key);
					return route;
				}
			}
		}
		return null;
	}

	public void setSelectedRoute(List<IRoute> routes, IRoute route) {
		String routesKey = getKey(routes);
		String routeKey = getKey(route);
		if(routeKey == null) {
			return; //should not happen, just a precaution
		}
		choices.put(routesKey, routeKey);
		history.remove(routesKey);
		history.add(routesKey);
		if(history.size() > HISTORY_LIMIT) {
			String ancient = history.remove(0);
			choices.remove(ancient);
		}
		save();
	}

	private void save() {
		StringBuilder preference = new StringBuilder();
		for (String _routesKey: history) {
			String _routeKey = choices.get(_routesKey);
			if(preference.length() > 0) preference.append(ITEMS_SEPARATOR);
			preference.append(_routesKey).append(ITEM_SEPARATOR).append(_routeKey);
		}
		preferences.putValue(SELECTED_ROUTE, preference.toString());
		try {
			if(preferences instanceof IPersistentPreferenceStore) {
				((IPersistentPreferenceStore)preferences).save();
			}
		} catch (IOException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	public void removeSelectedRoute(List<IRoute> routes) {
		String routesKey = getKey(routes);
		if(choices.containsKey(routesKey)) {
			choices.remove(routesKey);
			history.remove(routesKey);
			save();
		}
	}

	String getKey(List<IRoute> routes) {
		//sort to remove the dependency on order
		Set<String> set = new TreeSet<>();
		for (IRoute route: routes) {
			String key = getKey(route);
			if(key != null) set.add(key);
		}
		StringBuilder sb = new StringBuilder();
		for (String key: set) {
			if(sb.length() > 0) sb.append(KEY_SEPARATOR);
			sb.append(key);
		}
		return sb.toString();
	}

	String getKey(IRoute route) {
		return route.getURL();
	}
}
