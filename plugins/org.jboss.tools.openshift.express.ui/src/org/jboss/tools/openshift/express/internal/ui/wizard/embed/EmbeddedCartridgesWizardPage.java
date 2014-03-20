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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.wizard.IWizard;
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
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftResourceUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.viewer.EmbeddableCartridgeViewerSorter;
import org.jboss.tools.openshift.express.internal.ui.viewer.EqualityComparer;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author André Dietisheim
 */
public class EmbeddedCartridgesWizardPage extends AbstractOpenShiftWizardPage {

	private EmbeddedCartridgesWizardPageModel pageModel;
	private CheckboxTableViewer viewer;

	public EmbeddedCartridgesWizardPage(IEmbeddedCartridgesModel wizardModel, IWizard wizard) {
		super("Embed Cartridges", 
				NLS.bind("Please select the cartridges to embed into your application {0}",
						StringUtils.null2emptyString(wizardModel.getApplicationName())),
				"EmbedCartridgePage", wizard);
		this.pageModel = new EmbeddedCartridgesWizardPageModel(wizardModel);
	}

	public void setCheckedEmbeddableCartridges(List<IEmbeddableCartridge> embeddableCartridges) {
		pageModel.setCheckedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>(embeddableCartridges));
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
				ViewerProperties.checkedElements(IEmbeddableCartridge.class).observe(viewer),
				BeanProperties.set(
						EmbeddedCartridgesWizardPageModel.PROPERTY_CHECKED_EMBEDDABLE_CARTRIDGES)
						.observe(pageModel));
		// strategy has to be attached after the binding, so that the binding
		// can still add the checked cartridge and the strategy can correct
		viewer.addCheckStateListener(
				new EmbedCartridgeStrategyAdapter(pageModel, this, pageModel.getApplicationProperties()));

		IObservableValue selectedCartridgeObservable =
				BeanProperties.value(IEmbedCartridgesWizardPageModel.PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGE).observe(
						pageModel);
		ValueBindingBuilder
			.bind(ViewerProperties.singlePostSelection().observe(viewer))
			.to(selectedCartridgeObservable)
			.in(dbc);
		
		// uncheck all 
		Button uncheckAllButton = new Button(parent, SWT.PUSH);
		uncheckAllButton.setText("&Deselect All");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(uncheckAllButton);
		uncheckAllButton.addSelectionListener(onDeselectAll());

		// selected cartridge details
		Group cartridgeDetailsGroup = new Group(parent, SWT.NONE);
		cartridgeDetailsGroup.setText(" Selected Cartridge: ");
		GridDataFactory.fillDefaults()
				.span(2,1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 140).applyTo(cartridgeDetailsGroup);
		new EmbeddableCartridgeDetailViews(selectedCartridgeObservable, cartridgeDetailsGroup, dbc).createControls();
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
				.<IEmbeddableCartridge> column("Embeddable Cartridge")
				.weight(1)
				.labelProvider(new IColumnLabelProvider<IEmbeddableCartridge>() {

					@Override
					public String getValue(IEmbeddableCartridge cartridge) {
						return OpenShiftResourceUtils.toString(cartridge);
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
						setViewerInput(pageModel.loadEmbeddableCartridges());
						setViewerCheckedElements(pageModel.getCheckedEmbeddableCartridges());
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
		setViewerInput(new ArrayList<IEmbeddableCartridge>());
	}

	private void setViewerCheckedElements(final Collection<IEmbeddableCartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setCheckedElements(cartridges.toArray());
			}
		});
	}

	private void setViewerInput(final Collection<IEmbeddableCartridge> cartridges) {
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
				pageModel.setCheckedEmbeddableCartridges(new HashSet<IEmbeddableCartridge>());
			}
		};
	}

	public Set<IEmbeddableCartridge> getCheckedEmbeddableCartridges() {
		return pageModel.getCheckedEmbeddableCartridges();
	}

}