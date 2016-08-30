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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;


import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariablePage;

/**
 * Page to (mostly) edit the config items for a page
 * 
 * @author jeff.cantrill
 */
public class DeploymentConfigPage extends EnvironmentVariablePage {

	public static final String PAGE_NAME = "Deployment Config Settings Page";
	private static final String PAGE_TITLE = "Deployment Configuration && Scalability";
	private static final String PAGE_DESCRIPTION = "";

	private IDeploymentConfigPageModel model;
	private TableViewer dataViewer;

	//Layout
	private Composite volTableContainer;

	public DeploymentConfigPage(IWizard wizard, IDeploymentConfigPageModel model) {
		super(PAGE_TITLE, PAGE_DESCRIPTION, PAGE_NAME, wizard, model);
		this.model = model;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doCreateControls(final Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		//Env Variables Block
		createEnvVariableControl(parent, dbc, "Deployment environment variables (Runtime only):", EnvironmentVariablePage.TABLE_LABEL_TOOLTIP);

		Label separator1 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(separator1);

		createDataVolumeControl(parent, dbc);
		
		Label separator2 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(separator2);

		//Scaling
		Spinner replicas = new ScalingComponent().create(parent).getSpinner();
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(replicas))
			.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_REPLICAS)
			.observe(model))
			.in(dbc);
		parent.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				if(parent.isDisposed() || envTableContainer == null || envTableContainer.isDisposed()
						|| volTableContainer == null || volTableContainer.isDisposed()) {
					return;
				}

				int h = parent.getSize().y;
				if(h > 0) {
					int envtable = heightScale * 5 + 30; //Minimum height that can be assigned to envVars table.
					int voltable = heightScale * 4 + 24; //Minimum height that can be assigned to volumes table.
					int replicas = heightScale * 7; //Preferred height for bottom.
					int all = envtable + voltable + replicas;
					int minVolume = heightScale * 2;
	
					int hEnvVar = envtable;
					int hVolumes = voltable;
					if(h > all) {
						//In this case the bottom gets its preferred size and the remainder is proportionally shared by tables.
						hEnvVar = (h - replicas) * envtable / (envtable + voltable);
						hVolumes = (h - replicas) * voltable / (envtable + voltable);
					} else if(h > envtable + replicas + minVolume) {
						//Shrink volumes table, no use to shrink env-var table because of buttons.
						hVolumes = h - envtable - replicas;
					} else {
						//At a smaller available height, all components will be in lack of height evenly.
						hVolumes = minVolume;
					}
					((GridData)envTableContainer.getLayoutData()).heightHint = hEnvVar;
					((GridData)volTableContainer.getLayoutData()).heightHint = hVolumes;
					parent.layout(true);
				}
			}

			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void createDataVolumeControl(Composite parent, DataBindingContext dbc) {
		Composite sectionContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(sectionContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(6, 6).applyTo(sectionContainer);
		
		Label lblSection = new Label(sectionContainer, SWT.NONE);
		lblSection.setText("Data volumes:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.span(2,1)
			.applyTo(lblSection);
		Composite tableContainer = volTableContainer = new Composite(sectionContainer, SWT.NONE);
		
		dataViewer = createDataVolumeTable(tableContainer);
		dataViewer.setContentProvider(new ObservableListContentProvider());
		GridDataFactory.fillDefaults()
			.span(2, 4).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(dataViewer))
			.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_VOLUME)
			.observe(model));
		dataViewer.setInput(BeanProperties.list(
				IDeploymentConfigPageModel.PROPERTY_VOLUMES).observe(model));
		
		Label lblNotice = new Label(sectionContainer, SWT.WRAP);
		lblNotice.setText(NLS.bind("NOTICE: This image might use an EmptyDir volume. Data in EmptyDir volumes is not persisted across deployments.", model.getResourceName()));
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.grab(true, false)
			.span(2,2)
			.applyTo(lblNotice);
	}
	
	protected TableViewer createDataVolumeTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.dataViewer = new TableViewerBuilder(table, tableContainer)
				.column(new IColumnLabelProvider<String>() {
					@Override
					public String getValue(String label) {
						return label;
					}
				})
				.name("Container Path").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.buildViewer();
		dataViewer.setComparator(new ViewerComparator() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				String first = (String) e1;
				String other = (String) e2;
				return first.compareTo(other);
			}
			
		});
		return dataViewer;
	}
	
}
