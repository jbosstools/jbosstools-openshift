/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.dialog;

import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;

import com.openshift.restclient.model.route.IRoute;

/**
 * @author André Dietisheim
 */
public class SelectRouteDialog extends ElementListSelectionDialog {

	private boolean rememberChoice = false;
	private Button rememberChoiceButton = null;

	public SelectRouteDialog(List<IRoute> routes, Shell shell) {
		super(shell, new RouteLabelProvider());
		setTitle("Select Route");

		setMessage(createMessage(routes));

		setMultipleSelection(false);
		setAllowDuplicates(false);
		setElements(routes.toArray());
	}

	public SelectRouteDialog(List<IRoute> routes, Shell shell, boolean initialRememberChoice, IRoute initialSelection) {
		this(routes, shell);
		rememberChoice = initialRememberChoice;
		if (initialSelection != null) {
			setInitialSelections(new Object[] { initialSelection });
		}
	}

	private String createMessage(List<IRoute> routes) {
		StringBuilder message = new StringBuilder();
		if (!routes.isEmpty()) {
			IRoute route = routes.get(0);
			IConnection connection = ConnectionsRegistryUtil.safeGetConnectionFor(route);
			if (connection != null) {
				message.append("Server: ").append(connection.getUsername()).append(" ").append(connection.getHost())
						.append(StringUtils.getLineSeparator());
			}
			message.append("Project: ").append(route.getNamespaceName()).append(StringUtils.getLineSeparator());
			//Add more space between server/project info and instruction.
			message.append(StringUtils.getLineSeparator());
		}
		message.append("Select the route to open in a browser.");
		return message.toString();
	}

	public IRoute getSelectedRoute() {
		Object[] results = getResult();
		if (results == null || results.length < 1) {
			return null;
		} else {
			return (IRoute) results[0];
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);

		rememberChoiceButton = new Button(contents, SWT.CHECK);
		rememberChoiceButton.setText("Remember selected route");
		rememberChoiceButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (rememberChoice) {
			rememberChoiceButton.setSelection(rememberChoice);
		}
		rememberChoiceButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				rememberChoice = rememberChoiceButton.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		return contents;
	}

	public boolean isRememberChoice() {
		return rememberChoice;
	}

	public static class RouteLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return OpenShiftCommonImages.FILE;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof IRoute)) {
				return null;
			}

			IRoute route = (IRoute) element;
			return new StringBuilder().append(route.getName()).append(" (").append(route.getHost())
					.append(route.getPath()).append(")").toString();
		}

	}

}
