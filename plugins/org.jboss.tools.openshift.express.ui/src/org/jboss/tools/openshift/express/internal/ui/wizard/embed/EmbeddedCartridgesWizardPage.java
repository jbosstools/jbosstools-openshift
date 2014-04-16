/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftResourceLabelUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.viewer.EmbeddableCartridgeViewerSorter;
import org.jboss.tools.openshift.express.internal.ui.viewer.EqualityComparer;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ApplicationConfigurationWizardPageModel;

import com.openshift.client.cartridge.ICartridge;

/**
 * @author André Dietisheim
 */
public class EmbeddedCartridgesWizardPage extends AbstractOpenShiftWizardPage {

	private EmbeddedCartridgesWizardPageModel pageModel;
	private CheckboxTableViewer viewer;

	public EmbeddedCartridgesWizardPage(EmbeddedCartridgesWizardModel wizardModel, IWizard wizard) {
		this("Embed Cartridges", 
				NLS.bind("Please select the cartridges to embed into your application {0}",
						StringUtils.null2emptyString(wizardModel.getApplicationName())),
				wizardModel, wizard);
	}

	protected EmbeddedCartridgesWizardPage(String title, String description, EmbeddedCartridgesWizardModel wizardModel, IWizard wizard) {
		super(title, description,  "EmbedCartridgePage", wizard);
		this.pageModel = new EmbeddedCartridgesWizardPageModel(wizardModel);
	}

	public void setCheckedEmbeddableCartridges(List<ICartridge> embeddableCartridges) {
		pageModel.setCheckedEmbeddableCartridges(new HashSet<ICartridge>(embeddableCartridges));
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(10, 10).applyTo(parent);

		Composite tableContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).hint(500, 400).applyTo(tableContainer);
		this.viewer = createTable(tableContainer);
		dbc.bindSet(
				ViewerProperties.checkedElements(ICartridge.class).observe(viewer),
				BeanProperties.set(
						EmbeddedCartridgesWizardPageModel.PROPERTY_CHECKED_CARTRIDGES)
						.observe(pageModel));
		// strategy has to be attached after the binding, so that the binding
		// can still add the checked cartridge and the strategy can correct
		viewer.addCheckStateListener(onCartridgeChecked(pageModel, this));

		IObservableValue selectedCartridgeObservable =
				BeanProperties.value(EmbeddedCartridgesWizardPageModel.PROPERTY_SELECTED_CARTRIDGE)
						.observe(pageModel);
		ValueBindingBuilder
			.bind(ViewerProperties.singlePostSelection().observe(viewer))
			.to(selectedCartridgeObservable)
			.in(dbc);
		
		createButtons(parent, dbc);

		// selected cartridge details
		Group cartridgeDetailsGroup = new Group(parent, SWT.NONE);
		cartridgeDetailsGroup.setText(" Selected Cartridge: ");
		GridDataFactory.fillDefaults()
				.span(2,1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 140).applyTo(cartridgeDetailsGroup);
		new CartridgeDetailViews(
				selectedCartridgeObservable,
				BeanProperties
						.value(ApplicationConfigurationWizardPageModel.PROPERTY_CAN_ADDREMOVE_CARTRIDGES)
						.observe(pageModel),
				cartridgeDetailsGroup, dbc)
				.createControls();
	}

	protected void createButtons(Composite parent, DataBindingContext dbc) {
		// uncheck all 
		Button deselectAllButton = new Button(parent, SWT.PUSH);
		deselectAllButton.setText("&Deselect All");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(deselectAllButton);
		deselectAllButton.addSelectionListener(onDeselectAll());
	}

	protected ICheckStateListener onCartridgeChecked(EmbeddedCartridgesWizardPageModel pageModel, IWizardPage wizardPage) {
		return new FullfillRequirementsCheckStrategy(pageModel, wizardPage);
	}

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setLinesVisible(true);
		CheckboxTableViewer tableViewer = new CheckboxTableViewer(table);
		new TableViewerBuilder(tableViewer, tableContainer)
				.sorter(new EmbeddableCartridgeViewerSorter())
				.comparer(new EqualityComparer())
				.contentProvider(new ArrayContentProvider())
				.<ICartridge> column("Embeddable Cartridge")
				.weight(1)
				.labelProvider(new IColumnLabelProvider<ICartridge>() {

					@Override
					public String getValue(ICartridge cartridge) {
						return OpenShiftResourceLabelUtils.toString(cartridge);
					}
				})
				.buildColumn()
				.buildViewer();
		return tableViewer;
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading embeddable cartridges...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						setViewerInput(new ArrayList<ICartridge>(pageModel.loadEmbeddableCartridges()));
						setViewerCheckedElements(new ArrayList<ICartridge>(pageModel.getCheckedCartridges()));
						return Status.OK_STATUS;
					} catch (Exception e) {
						clearViewer();
						return OpenShiftUIActivator.createErrorStatus("Could not load embeddable cartridges", e);
					}
				}

			}, getContainer(), getDataBindingContext());
		} catch (Exception e) {
			// ignore
		}
	}

	private void clearViewer() {
		setViewerInput(new ArrayList<ICartridge>());
	}

	private void setViewerCheckedElements(final Collection<ICartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setCheckedElements(cartridges.toArray());
			}
		});
	}

	private void setViewerInput(final Collection<ICartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setInput(cartridges);
			}
		});
	}

	private SelectionListener onDeselectAll() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if(MessageDialog.openQuestion(getShell(), 
						Messages.DESELECT_ALL_CARTRIDGES_TITLE,
						Messages.DESELECT_ALL_CARTRIDGES_DESCRIPTION
					)) {
					pageModel.setCheckedEmbeddableCartridges(new HashSet<ICartridge>());
				}
			}
		};
	}

	public Set<ICartridge> getCheckedCartridges() {
		return pageModel.getCheckedCartridges();
	}
}