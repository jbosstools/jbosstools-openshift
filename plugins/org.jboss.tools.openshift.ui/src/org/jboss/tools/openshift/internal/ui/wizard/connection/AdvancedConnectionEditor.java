/*******************************************************************************
 * Copyright (c) 2016-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionFactory;
import org.jboss.tools.openshift.core.connection.registry.RegistryProviderModel;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.core.preferences.OpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAdvancedPropertiesProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.IAdvancedConnectionPropertiesEditor;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimTrailingSlashConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.internal.common.ui.detailviews.BaseDetailsView;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DialogAdvancedPart;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinaryValidationJob;
import org.jboss.tools.openshift.internal.ui.validator.URLValidator;

@SuppressWarnings("rawtypes")
public class AdvancedConnectionEditor extends BaseDetailsView implements IAdvancedConnectionPropertiesEditor {

	private static final String VALIDATOR_URL_TYPE = "image registry";
	private static final int DELAY = 500;
	private AdvancedConnectionEditorModel model;
	private ConnectionWizardPageModel pageModel;
	private IObservableValue selectedConnection;
	private IValueChangeListener connectionChangedListener;
	private IObservableValue<String> registryURLObservable;
	private IObservableValue clusterNamespaceObservable;
	private IConnectionAdvancedPropertiesProvider connectionAdvancedPropertiesProvider;
	private Map<String, Object> extendedProperties = null;
	private IObservableValue<IStatus> ocLocationValidity = new WritableValue<>(Status.OK_STATUS, IStatus.class);
	private IObservableValue<IStatus> ocVersionValidity = new WritableValue<>(Status.OK_STATUS, IStatus.class);
	
	@SuppressWarnings("unchecked")
	@Override
	public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {

		this.pageModel = (ConnectionWizardPageModel) context;
		this.selectedConnection = BeanProperties.value(ConnectionWizardPageModel.PROPERTY_SELECTED_CONNECTION)
				.observe(pageModel);
		this.model = new AdvancedConnectionEditorModel();

		Composite composite = setControl(new Composite(parent, SWT.None));
		GridLayoutFactory.fillDefaults().applyTo(composite);

		DialogAdvancedPart part = new AdvancedConnectionPart(dbc);
		part.create(composite, 1);
		this.connectionAdvancedPropertiesProvider = new ConnectionAdvancedPropertiesProvider();
		updateOcObservables();
		return composite;
	}

	private void updateOcObservables() {
		boolean override = model.getOcOverride();
		String location;
		if (override) {
			location = model.getOcOverrideLocation();
		} else {
			location = OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		}
		updateOcObservables(location);
	}

	private void updateOcObservables(String location) {
		ocVersionValidity.setValue(ValidationStatus.cancel("Verifying oc version..."));
		OCBinaryValidationJob job = new OCBinaryValidationJob(location);
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				ocVersionValidity.getRealm().exec(() -> ocVersionValidity.setValue(job.getOCVersionValidity()));
			}
		});
		job.schedule();
	}

	protected void onSelectedConnectionChanged() {
		registryURLObservable.setValue(model.getRegistryURL());
	}

	@Override
	public void onVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		pageModel.setConnectionAdvancedPropertiesProvider(this.connectionAdvancedPropertiesProvider);
		this.connectionChangedListener = addSelectedConnectionChangedListener(selectedConnection, dbc);
	}

	@Override
	public void onInVisible(IObservableValue detailViewModel, DataBindingContext dbc) {
		pageModel.setConnectionAdvancedPropertiesProvider(null);
		removeConnectionChangedListener(connectionChangedListener, selectedConnection);
		this.connectionChangedListener = null;
	}

	@SuppressWarnings("unchecked")
	private IValueChangeListener addSelectedConnectionChangedListener(final IObservableValue selectedConnection,
			final DataBindingContext dbc) {
		IValueChangeListener listener = (ValueChangeEvent event) -> {
				onSelectedConnectionChanged();
				DataBindingUtils.validateTargetsToModels(dbc);
		};
		selectedConnection.addValueChangeListener(listener);
		return listener;
	}

	@SuppressWarnings("unchecked")
	private void removeConnectionChangedListener(final IValueChangeListener connectionChangedListener,
			final IObservableValue selectedConnection) {
		if (selectedConnection == null || selectedConnection.isDisposed() || connectionChangedListener == null) {
			return;
		}

		selectedConnection.removeValueChangeListener(connectionChangedListener);
	}

	@Override
	public boolean isViewFor(Object object) {
		return object instanceof ConnectionFactory;
	}

	public Map<String, Object> getExtendedProperties() {
		if (extendedProperties != null) {
			return extendedProperties;
		}
		IConnection connection = pageModel.getSelectedConnection();
		if (connection != null) {
			if (NewConnectionMarker.getInstance() == connection) {
				extendedProperties = new HashMap<>();
			} else if (connection instanceof Connection) {
				extendedProperties = ((Connection) connection).getExtendedProperties();
			}
			return extendedProperties;
		}
		return null;
	}

	class AdvancedConnectionEditorModel extends ObservablePojo {

		static final String PROP_REGISTRY_URL = "registryURL";
		static final String PROP_CLUSTER_NAMESPACE = "clusterNamespace";

		// custom oc location
		static final String PROP_OC_OVERRIDE = "ocOverride";
		static final String PROP_OC_OVERRIDE_LOCATION = "ocOverrideLocation";

		public void setOcOverride(boolean value) {
			Map<String, Object> properties = getExtendedProperties();
			if (properties != null) {
				Object old = properties.get(ICommonAttributes.OC_OVERRIDE_KEY);
				properties.put(ICommonAttributes.OC_OVERRIDE_KEY, value);
				firePropertyChange(PROP_OC_OVERRIDE, old, value);
			}
		}

		public boolean getOcOverride() {
			Map<String, Object> properties = getExtendedProperties();
			if (properties != null) {
				return ((Boolean) (ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.OC_OVERRIDE_KEY), false)))
						.booleanValue();
			}
			return false;
		}

		public void setOcOverrideLocation(String value) {
			Map<String, Object> properties = getExtendedProperties();
			if (properties != null) {
				Object old = properties.get(ICommonAttributes.OC_LOCATION_KEY);
				properties.put(ICommonAttributes.OC_LOCATION_KEY, value);
				firePropertyChange(PROP_OC_OVERRIDE_LOCATION, old, value);
			}
		}

		public String getOcOverrideLocation() {
			if (getOcOverride()) {
				Map<String, Object> properties = getExtendedProperties();
				if (properties != null) {
					return (String) ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.OC_LOCATION_KEY), "");
				}
			}
			return OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
		}

		public void setRegistryURL(String value) {
			Map<String, Object> properties = getExtendedProperties();
			if (properties != null) {
				Object old = properties.get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY);
				properties.put(ICommonAttributes.IMAGE_REGISTRY_URL_KEY, value);
				firePropertyChange(PROP_REGISTRY_URL, old, value);
			}
		}

		public String getRegistryURL() {
			Map<String, Object> properties = getExtendedProperties();
			if (properties != null) {
				return (String) ObjectUtils.defaultIfNull(properties.get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY), "");
			}
			return "";
		}

		public void setClusterNamespace(String value) {
			Map<String, Object> properties = getExtendedProperties();
			if (properties != null) {
				Object old = properties.get(ICommonAttributes.CLUSTER_NAMESPACE_KEY);
				properties.put(ICommonAttributes.CLUSTER_NAMESPACE_KEY, value);
				firePropertyChange(PROP_CLUSTER_NAMESPACE, old, value);
			}
		}

		public String getClusterNamespace() {
			Map<String, Object> properties = getExtendedProperties();
			if (properties != null) {
				return (String) properties.getOrDefault(ICommonAttributes.CLUSTER_NAMESPACE_KEY,
						ICommonAttributes.COMMON_NAMESPACE);
			}
			return ICommonAttributes.COMMON_NAMESPACE;
		}
	}

	private class ConnectionAdvancedPropertiesProvider implements IConnectionAdvancedPropertiesProvider {

		@Override
		public IConnection update(IConnection conn) {
			Assert.isLegal(conn instanceof Connection);

			final Connection connection = (Connection) conn;
			Display.getDefault().syncExec(() -> {
					connection.setExtendedProperty(ICommonAttributes.IMAGE_REGISTRY_URL_KEY,
							registryURLObservable.getValue());
					connection.setExtendedProperty(ICommonAttributes.CLUSTER_NAMESPACE_KEY,
							clusterNamespaceObservable.getValue());
			});
			return connection;
		}

	}

	@Override
	public void saveChanges(ConnectionWizardPageModel pageModel) {
		IConnection c = pageModel.getConnection();
		if (c instanceof Connection && getExtendedProperties() != null) {
			((Connection) c).setExtendedProperties(getExtendedProperties());
		}
	}

	private class AdvancedConnectionPart extends DialogAdvancedPart {
		
		private DataBindingContext dbc;

		private AdvancedConnectionPart(DataBindingContext dbc) {
			this.dbc = dbc;
		}

		@Override
		protected void createAdvancedContent(Composite advancedComposite) {
			Label lblRegistry = new Label(advancedComposite, SWT.NONE);
			lblRegistry.setText("Image Registry URL:");
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(lblRegistry);

			Text txtRegistry = new Text(advancedComposite, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(1, 1)
					.applyTo(txtRegistry);

			Button registryDiscover = new Button(advancedComposite, SWT.PUSH);
			registryDiscover.setText("Discover...");
			registryDiscover.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					discoverRegistryPressed(registryDiscover.getShell());
				}
			});
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(registryDiscover);
			UIUtils.setDefaultButtonWidth(registryDiscover);

			registryURLObservable = WidgetProperties.text(SWT.Modify).observeDelayed(DELAY, txtRegistry);
			Binding registryURLBinding = ValueBindingBuilder.bind(registryURLObservable)
					.validatingAfterConvert(new URLValidator(VALIDATOR_URL_TYPE, true))
					.converting(new TrimTrailingSlashConverter())
					.to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_REGISTRY_URL).observe(model))
					.in(dbc);
			ControlDecorationSupport.create(registryURLBinding, SWT.LEFT | SWT.TOP);

			Label lblNamespace = new Label(advancedComposite, SWT.NONE);
			lblNamespace.setText("Cluster namespace:");
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(lblNamespace);

			Text txtClusterNamespace = new Text(advancedComposite, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
					.applyTo(txtClusterNamespace);

			clusterNamespaceObservable = WidgetProperties.text(SWT.Modify).observeDelayed(DELAY,
					txtClusterNamespace);
			ValueBindingBuilder.bind(clusterNamespaceObservable).converting(new TrimmingStringConverter())
					.to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_CLUSTER_NAMESPACE).observe(model))
					.in(dbc);

			Link ocWorkspace = new Link(advancedComposite, SWT.PUSH);
			ocWorkspace.setText("<a>Workspace OC Settings</a>");
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(ocWorkspace);

			// Override OC location widgets
			Button overrideOC = new Button(advancedComposite, SWT.CHECK);
			overrideOC.setText("Override 'oc' location: ");
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(lblRegistry);
			IObservableValue<Boolean> overrideOCObservable = WidgetProperties.selection().observe(overrideOC);
			ValueBindingBuilder.bind(overrideOCObservable)
					.to(BeanProperties.value(AdvancedConnectionEditorModel.PROP_OC_OVERRIDE).observe(model))
					.in(dbc);

			final Text ocText = new Text(advancedComposite, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(ocText);
			IObservableValue<String> ocLocationObservable = WidgetProperties.text(SWT.Modify).observe(ocText);
			Binding ocLocationBinding = ValueBindingBuilder.bind(ocLocationObservable)
					.converting(new TrimmingStringConverter()).to(BeanProperties
							.value(AdvancedConnectionEditorModel.PROP_OC_OVERRIDE_LOCATION).observe(model))
					.in(dbc);

			overrideOCObservable.addValueChangeListener(
					(ValueChangeEvent<? extends Boolean> event) -> updateOcObservables()
			);

			ocLocationBinding.getValidationStatus().addValueChangeListener(
					(ValueChangeEvent<? extends IStatus> event) -> updateOcObservables()
			);
			ValueBindingBuilder.bind(overrideOCObservable).to(WidgetProperties.enabled().observe(ocText))
					.notUpdatingParticipant().in(dbc);

			Button ocBrowse = new Button(advancedComposite, SWT.PUSH);
			ocBrowse.setText("Browse...");
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(ocBrowse);
			UIUtils.setDefaultButtonWidth(ocBrowse);

			ocWorkspace.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferenceDialog pd = PreferencesUtil.createPreferenceDialogOn(null,
							IOpenShiftCoreConstants.OPEN_SHIFT_PREFERENCE_PAGE_ID, new String[] {}, null);
					pd.open();
					if (!overrideOC.getSelection()) {
						String ocLoc = OpenShiftCorePreferences.INSTANCE.getOCBinaryLocation();
						String nullsafe = ocLoc == null ? "" : ocLoc;
						ocText.setText(nullsafe);
					}
					updateOcObservables();
				}
			});

			// Validation here is done via a listener rather than dbc validators
			// because dbc validators will validate in the UI thread, but validation
			// of this field requires a background job.
			ocBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fd = new FileDialog(ocBrowse.getShell());
					fd.setText(ocText.getText());
					IPath p = new Path(ocText.getText());
					if (p.segmentCount() > 1) {
						fd.setFilterPath(p.removeLastSegments(1).toOSString());
					}
					String result = fd.open();
					if (result != null) {
						ocLocationObservable.setValue(result);
					}
				}
			});

			ValueBindingBuilder.bind(overrideOCObservable).to(WidgetProperties.enabled().observe(ocBrowse))
					.notUpdatingParticipant().in(dbc);

			ValidationStatusProvider ocValidator = new MultiValidator() {
				
				@Override
				public IObservableList getTargets() {
					WritableList targets = new WritableList();
					targets.add(ocLocationObservable);
					targets.add(ocLocationValidity);
					targets.add(ocVersionValidity);
					return targets;
				}

				@Override
				protected IStatus validate() {
					// access all observables that the framework should listen to
					IStatus ocLocationStatus = ocLocationValidity.getValue();
					IStatus ocVersionStatus = ocVersionValidity.getValue();

					IStatus status;
					if (!ocLocationStatus.isOK()) {
						status = ocLocationStatus;
					} else {
						status = ocVersionStatus;
					}
					return status;
				}
			};

			dbc.addValidationStatusProvider(ocValidator);
			ControlDecorationSupport.create(ocValidator, SWT.LEFT | SWT.TOP, null,
					new RequiredControlDecorationUpdater());
		}

		private void discoverRegistryPressed(Shell shell) {
			IConnection tmp = pageModel.createConnection();
			IStatus ret = RegistryProviderModel.getDefault().getRegistryURL(tmp);

			if (ret != null && ret.isOK()) {
				String oldVal = registryURLObservable.getValue();
				String newVal = ret.getMessage();
				// If they're equal, do nothing
				if (!StringUtils.equals(oldVal, newVal)) {
					// Verify with user
					String title = "Overwrite registry URL?";
					String msg = "Are you sure you want to change the registry URL from " + oldVal + " to " + newVal + "?";
					MessageDialog dialog = new MessageDialog(shell, title, null, msg, MessageDialog.CONFIRM,
							new String[] { "OK", "Cancel" }, 0);
					String old = registryURLObservable.getValue().trim();
					if (old.isEmpty() || dialog.open() == IDialogConstants.OK_ID) {
						registryURLObservable.setValue(ret.getMessage());
					}
				}
			} else {
				String title = "Registry URL not found";
				String msg = "No registry provider found for the given connection. If your Openshift connection is backed by a CDK or minishift installation, please ensure the CDK is running.";
				ErrorDialog ed = new ErrorDialog(shell, title, msg, ret,
						IStatus.ERROR | IStatus.WARNING | IStatus.INFO | IStatus.CANCEL);
				ed.open();
			}
		}

		@Override
		protected GridLayoutFactory adjustAdvancedCompositeLayout(GridLayoutFactory gridLayoutFactory) {
			return gridLayoutFactory.numColumns(3);
		}
	}
}
