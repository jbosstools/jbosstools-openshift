/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinary;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.authorization.IAuthorizationContext;

public class ChooseOpenshiftConnectionComposite extends Composite {
	private IConnectionChangedListener connectionChangeListener;

	private Combo connectionCombo;
	private Label serverValueLbl;
	private Label protocolValLbl;
	private Label usernameValLbl;
	private Label imageRegistryValLbl;
	private Label clusterNamespaceValLbl;
	private Label ocLocationValLbl;

	private List<IConnection> connections;
	private IConnection selectedConnection;
	private ControlDecoration ocLocationDecorator;

	public ChooseOpenshiftConnectionComposite(Composite parent) {
		super(parent, SWT.NONE);
		createComposite(this);
	}

	public Composite createComposite(Composite main) {
		main.setLayout(new GridLayout(5, true));
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		main.setLayoutData(gd);

		Label connLabel = new Label(main, SWT.NONE);
		connLabel.setText("Connection: ");

		connectionCombo = new Combo(main, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(connectionCombo);

		Composite btnWrapper = new Composite(main, SWT.NONE);
		btnWrapper.setLayout(new FillLayout());
		Button editConBtn = new Button(btnWrapper, SWT.PUSH);
		editConBtn.setText("Edit...");
		Button addConBtn = new Button(btnWrapper, SWT.PUSH);
		addConBtn.setText("New...");
		GridDataFactory.fillDefaults().applyTo(btnWrapper);

		addConBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final ConnectionWizard connectionWizard = new ConnectionWizard((IConnection) null);
				int ret = WizardUtils.openWizardDialog(connectionWizard, addConBtn.getShell());
				if (ret == Window.OK) {
					refreshConnections();
					setConnectionInConnectionWizardCombo(connectionWizard.getConnection());
				}
			}
		});

		editConBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final ConnectionWizard connectionWizard = new ConnectionWizard(selectedConnection,
						ConnectionWizard.EDIT_CONNECTION_TITLE);
				WizardUtils.openWizardDialog(connectionWizard, editConBtn.getShell());
				refreshConnections();
				IConnection c = connectionWizard.getConnection();
				setConnectionInConnectionWizardCombo(c);
				setSelectedConnection(c);
			}
		});

		Label serverLbl = new Label(main, SWT.NONE);
		serverLbl.setText("Server: ");
		serverValueLbl = new Label(main, SWT.NONE);
		GridDataFactory.fillDefaults().span(4, 1).applyTo(serverValueLbl);

		Group authGroup = new Group(main, SWT.NONE);
		authGroup.setText("Authentication");
		GridDataFactory.fillDefaults().span(5, 1).indent(0, 5).applyTo(authGroup);
		authGroup.setLayout(new GridLayout(6, true));

		Label protocolLbl = new Label(authGroup, SWT.NONE);
		protocolLbl.setText("Protocol: ");
		protocolValLbl = new Label(authGroup, SWT.NONE);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(protocolValLbl);

		Label userLbl = new Label(authGroup, SWT.NONE);
		userLbl.setText("Username: ");
		usernameValLbl = new Label(authGroup, SWT.NONE);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(usernameValLbl);

		Group advancedGroup = new Group(main, SWT.NONE);
		advancedGroup.setText("Advanced");
		GridDataFactory.fillDefaults().span(5, 1).indent(0, 5).applyTo(advancedGroup);
		advancedGroup.setLayout(new GridLayout(6, true));

		Label imageRegistryLbl = new Label(advancedGroup, SWT.NONE);
		imageRegistryLbl.setText("Image Registry URL: ");
		imageRegistryValLbl = new Label(advancedGroup, SWT.NONE);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(imageRegistryValLbl);

		Label clusterNamespaceLbl = new Label(advancedGroup, SWT.NONE);
		clusterNamespaceLbl.setText("Cluster namespace: ");
		clusterNamespaceValLbl = new Label(advancedGroup, SWT.NONE);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(clusterNamespaceValLbl);

		Label ocLocationLbl = new Label(advancedGroup, SWT.NONE);
		ocLocationLbl.setText("OC Binary Location: ");
		ocLocationValLbl = new Label(advancedGroup, SWT.NONE);
		ocLocationDecorator = new ControlDecoration(ocLocationValLbl, SWT.TOP | SWT.LEFT);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(ocLocationValLbl);

		// Load the model
		refreshConnections();

		connectionCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selIndex = connectionCombo.getSelectionIndex();
				if (selIndex != -1 && selIndex < connections.size()) {
					setSelectedConnection(connections.get(selIndex));
				}
				editConBtn.setEnabled(selIndex != -1);
			}
		});
		if (connectionCombo.getItemCount() > 0) {
			IConnection recentConnection = ConnectionsRegistrySingleton.getInstance().getRecentConnection();
			setConnectionInConnectionWizardCombo(recentConnection);
			setSelectedConnection(recentConnection);
		}
		int selIndex = connectionCombo.getSelectionIndex();
		editConBtn.setEnabled(selIndex != -1);
		return main;
	}

	private void validateOCLocation() {
		if (selectedConnection != null) {
			String ocValString = OCBinary.getInstance().getPath(selectedConnection);
			if (StringUtils.isEmpty(ocValString)) {
				ocLocationDecorator.show();
				ocLocationDecorator.setImage(FieldDecorationRegistry.getDefault()
						.getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
				ocLocationDecorator.setDescriptionText(
						"The selected connection does not have an 'oc' command associated with it. Please edit the connection or modify your workspace settings to add one.");
			} else {
				ocLocationDecorator.hide();
			}
		}
	}

	private void refreshConnections() {
		String selectedName = selectedConnection == null ? null : createLabel(selectedConnection);

		Collection<IConnection> allCons = ConnectionsRegistrySingleton.getInstance().getAll();
		connections = allCons.stream().filter(connection -> connection instanceof IOpenShiftConnection)
				.collect(Collectors.toList());
		List<String> connectionNames = new ArrayList<>();
		allCons.forEach(con -> connectionNames.add(createLabel(con)));
		connectionCombo.setItems((String[]) connectionNames.toArray(new String[connectionNames.size()]));

		if (selectedName != null && connectionNames.contains(selectedName)) {
			 setConnectionInConnectionWizardCombo(connectionNames.indexOf(selectedName));
		}
	}

	private void setSelectedConnection(IConnection con) {
		selectedConnection = con;
		if (selectedConnection != null) {
			serverValueLbl.setText(con.getHost());
			if (con instanceof IOpenShiftConnection) {
				String authScheme = ((Connection) con).getAuthScheme();
				protocolValLbl.setText(authScheme);
				if (IAuthorizationContext.AUTHSCHEME_BASIC.equals(authScheme)) {
					usernameValLbl.setText(con.getUsername());
				} else {
					usernameValLbl.setText("Not applicable");
				}

				Map<String, Object> eProps = ((IOpenShiftConnection) con).getExtendedProperties();
				Object imgReg = eProps.get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY);
				String imgRegVal = (imgReg == null ? "" : imgReg.toString());
				imageRegistryValLbl.setText(imgRegVal);
				clusterNamespaceValLbl.setText(((Connection) con).getClusterNamespace());
			} else {
				protocolValLbl.setText("");
				imageRegistryValLbl.setText("");
				clusterNamespaceValLbl.setText("");
			}
			String ocValString = OCBinary.getInstance().getPath(con);
			if (ocValString == null)
				ocValString = "";
			ocLocationValLbl.setText(ocValString);
			validateOCLocation();
		} else {
			serverValueLbl.setText("");
			protocolValLbl.setText("");
			usernameValLbl.setText("");
			imageRegistryValLbl.setText("");
			clusterNamespaceValLbl.setText("");
			ocLocationValLbl.setText("");
		}

		if (connectionChangeListener != null) {
			connectionChangeListener.connectionChanged(selectedConnection);
		}
	}

	private String createLabel(IConnection connection) {
		StringBuilder builder = new StringBuilder();
		String username = connection.getUsername();
		if (!StringUtils.isEmpty(username)) {
			builder.append(connection.getUsername()).append(" - ");
		}
		builder.append(connection.getHost());
		return builder.toString();
	}

	public Job getConnectJob() {
		return new Job("Connecting to OpenShift") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String blockedMsg = runConnectionOrError();
					if (blockedMsg == null) {
						return Status.OK_STATUS;
					}
					return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, blockedMsg);
				} catch (IOException | OpenShiftException ioe) {
					return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
							"Error while connecting to OpenShift: " + ioe.getMessage(), ioe);
				}
			}

			private String runConnectionOrError() throws IOException {
				String blockedMsg = null;
				if (selectedConnection != null && selectedConnection.canConnect()) {
					boolean connected = selectedConnection.connect();
					if (!connected) {
						blockedMsg = "Unable to connect to OpenShift connection";
					}
				} else if (selectedConnection == null) {
					blockedMsg = "Please select an OpenShift connection.";
				} else {
					blockedMsg = "Unable to connect to selected OpenShift Connection";
				}
				return blockedMsg;
			}
		};
	}
	
	private void setConnectionInConnectionWizardCombo(IConnection connection) {
		setConnectionInConnectionWizardCombo(connections.indexOf(connection));
	}
	
	private void setConnectionInConnectionWizardCombo(int ind) {
		if (ind != -1) {
			connectionCombo.select(ind);
		}
	}

	public static interface IConnectionChangedListener {
		public void connectionChanged(IConnection newVal);
	}

	public void setConnectionChangeListener(IConnectionChangedListener connectionChangeListener) {
		this.connectionChangeListener = connectionChangeListener;
	}

	public IConnection getConnection() {
		return selectedConnection;
	}
}
