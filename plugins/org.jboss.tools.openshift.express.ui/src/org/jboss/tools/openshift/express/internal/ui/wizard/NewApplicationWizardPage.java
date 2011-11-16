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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateListStrategy;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.client.Cartridge;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.IEmbeddableCartridge;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class NewApplicationWizardPage extends AbstractOpenShiftWizardPage {

	private NewApplicationWizardPageModel model;
	private CheckboxTableViewer viewer;

	public NewApplicationWizardPage(NewApplicationWizardPageModel model, IWizard wizard) {
		super("Create new OpenShift Express application", "Create new OpenShift Express application",
				"Create new OpenShift Express application", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(parent);

		Label nameLabel = new Label(parent, SWT.NONE);
		nameLabel.setText("Na&me");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);
		Text nameText = new Text(parent, SWT.BORDER);
		nameText.setTextLimit(13);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(nameText);
		Binding nameBinding = dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(nameText)
				, BeanProperties.value(NewApplicationWizardPageModel.PROPERTY_NAME).observe(model)
				, new UpdateValueStrategy().setAfterGetValidator(new ApplicationNameValidator())
				, null);
		ControlDecorationSupport.create(nameBinding, SWT.LEFT | SWT.TOP);

		Label cartridgeLabel = new Label(parent, SWT.WRAP);
		cartridgeLabel.setText("&Application Type");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(cartridgeLabel);
		Combo cartridgesCombo = new Combo(parent, SWT.BORDER |
				SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(cartridgesCombo);
		dbc.bindList(
				WidgetProperties.items().observe(cartridgesCombo)
				, BeanProperties.list(NewApplicationWizardPageModel.PROPERTY_CARTRIDGES).observe(model)
				, new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER)
				, new UpdateListStrategy().setConverter(new Converter(Object.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (!(fromObject instanceof ICartridge)) {
							return null;
						}
						return ((ICartridge) fromObject).getName();
					}
				}));
		Binding comboSelectionBinding = dbc.bindValue(
				WidgetProperties.selection().observe(cartridgesCombo)
				, BeanProperties.value(NewApplicationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE).observe(model)
				, new UpdateValueStrategy().setConverter(new Converter(String.class, ICartridge.class) {

					@Override
					public Object convert(Object fromObject) {
						if (fromObject instanceof String
								&& ((String) fromObject).length() > 0) {
							return new Cartridge(((String) fromObject));
						}
						return null;
					}
				}).setAfterGetValidator(
						new IValidator() {

							@Override
							public IStatus validate(Object value) {
								if (!(value instanceof String)
										|| ((String) value).length() == 0) {
									return ValidationStatus.error("You have to select a type");
								} else {
									return ValidationStatus.ok();
								}
							}
						})
				, new UpdateValueStrategy().setConverter(new Converter(ICartridge.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (fromObject instanceof ICartridge) {
							return ((ICartridge) fromObject).getName();
						}
						return null;
					}
				}));
		ControlDecorationSupport.create(comboSelectionBinding, SWT.LEFT | SWT.TOP);

		createEmbedGroup(parent);
	}

	private void createEmbedGroup(Composite parent) {
		Group embedGroup = new Group(parent, SWT.NONE);
		embedGroup.setText("Embeddable Cartridges");
		GridDataFactory.fillDefaults()
				.hint(300, 150).align(SWT.FILL, SWT.FILL).span(2, 1).grab(true, true)
				.applyTo(embedGroup);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 6;
		fillLayout.marginWidth = 6;
		embedGroup.setLayout(fillLayout);

		Composite tableContainer = new Composite(embedGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		viewer.addCheckStateListener(onEmbeddableCartridgeSelected());
	}

	private ICheckStateListener onEmbeddableCartridgeSelected() {
		return new ICheckStateListener() {
			
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) event.getElement();
				if (event.getChecked()) {
					model.getSeleEmbeddableCartridges().add(cartridge);
				} else {
					model.getSeleEmbeddableCartridges().remove(cartridge);
				}
			}
		};
	}

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());

		createTableColumn("Embeddable Cartridge", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) cell.getElement();
				cell.setText(cartridge.getName());
			}
		}, viewer, tableLayout);
		return viewer;
	}

	private void createTableColumn(String name, int weight, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.setLabelProvider(cellLabelProvider);

		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading cartridges...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						model.loadCartridges();
					} catch (OpenShiftException e) {
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Could not load cartridges", e);
					}
					return Status.OK_STATUS;
				}
			}, getContainer());
		} catch (Exception e) {
			// ignore
		}

		try {
			WizardUtils.runInWizard(new Job("Loading embeddable cartridges...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						setCartridgesViewerInput(model.loadEmbeddableCartridges());
						return Status.OK_STATUS;
					} catch (Exception e) {
						clearCartridgesViewer();
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Could not load embeddable cartridges", e);
					}
				}

			}, getContainer(), getDataBindingContext());
		} catch (Exception e) {
			// ignore
		}

	}

	private void clearCartridgesViewer() {
		setCartridgesViewerInput(new ArrayList<IEmbeddableCartridge>());
	}

	private void setCartridgesViewerInput(final Collection<IEmbeddableCartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setInput(cartridges);
			}
		});
	}

	private class ApplicationNameValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			String name = (String) value;
			if (name.length() == 0) {
				return ValidationStatus.error("You have to provide a name");
			} else if (model.hasApplication(name)) {
				return ValidationStatus.error(NLS.bind(
						"Names must be unique. You already have an application named \"{0}\"", name));
			}
			return ValidationStatus.ok();
		}
	}

}