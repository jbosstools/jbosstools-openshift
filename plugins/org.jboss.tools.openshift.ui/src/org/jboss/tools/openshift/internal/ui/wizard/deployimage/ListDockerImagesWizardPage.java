/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.ListDockerImagesWizardModel.DockerImageTag;

/**
 * {@link WizardPage} to list and select a Docker Image.
 */
public class ListDockerImagesWizardPage extends AbstractOpenShiftWizardPage {

	/** the model. */
	private final ListDockerImagesWizardModel model;

	private static String LIST_DOCKER_IMAGES_PAGE_NAME = "List Docker Images Page";

	private static final String PAGE_DESCRIPTION = "This page allows you to choose a local image and the name to be used for the deployed resources.";

	/**
	 * Constructor.
	 * 
	 * @param wizard
	 *            the parent wizard
	 * @param model
	 *            the model
	 */
	public ListDockerImagesWizardPage(final IWizard wizard, final ListDockerImagesWizardModel model) {
		super("Deploy an Image", PAGE_DESCRIPTION, LIST_DOCKER_IMAGES_PAGE_NAME, wizard);
		this.model = model;
	}

	@Override
	public boolean isPageComplete() {
		// can finish if a Docker image was selected
		return this.model.getSelectedDockerImage() != null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doCreateControls(final Composite parent, final DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(parent);

		// filter image by name
		final Label filterByNameLabel = new Label(parent, SWT.SEARCH);
		filterByNameLabel.setText("Filter:");
		filterByNameLabel.setToolTipText("Filter images by their name");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(filterByNameLabel);
		final Text filterByNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(filterByNameText);
		
		// table with all images
		final Table dockerImagesTable = new Table(parent,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		final TableViewer dockerImagesTableViewer = new TableViewer(dockerImagesTable);
		dockerImagesTable.setHeaderVisible(true);
		dockerImagesTable.setLinesVisible(true);
		addTableViewerColum(dockerImagesTableViewer, "Name", SWT.NONE, SWT.LEFT, 200, new ColumnLabelProvider() {

			@Override
			public String getText(final Object element) {
				return ((DockerImageTag) element).getRepoName();
			}
		});
		addTableViewerColum(dockerImagesTableViewer, "Tag", SWT.NONE, SWT.LEFT, 100, new ColumnLabelProvider() {

			@Override
			public String getText(final Object element) {
				return ((DockerImageTag) element).getTag();
			}
		});
		addTableViewerColum(dockerImagesTableViewer, "Image ID", SWT.NONE, SWT.LEFT, 150, new ColumnLabelProvider() {

			@Override
			public String getText(final Object element) {
				return ((DockerImageTag) element).getId();
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(2, 1).hint(200, 100)
				.applyTo(dockerImagesTable);

		// observe the viewer content
		dockerImagesTableViewer.setContentProvider(new ObservableListContentProvider());
		// observe the viewer content
		dockerImagesTableViewer.setInput(BeanProperties
				.list(ListDockerImagesWizardModel.class, ListDockerImagesWizardModel.DOCKER_IMAGES).observe(model));

		// filter by name
		final ViewerFilter imageNameFilter = new ViewerFilter() {
			
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return ((DockerImageTag)element).getRepoName().contains(filterByNameText.getText());
			}
		}; 
		dockerImagesTableViewer.addFilter(imageNameFilter);
		filterByNameText.addModifyListener(onFilterImages(dockerImagesTableViewer));

		// bind selection
		dbc.bindValue(ViewerProperties.singleSelection().observe(dockerImagesTableViewer),
				BeanProperties.value(ListDockerImagesWizardModel.SELECTED_DOCKER_IMAGE).observe(model));
		
		dockerImagesTableViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				IWizardContainer container = getWizard().getContainer();
				if(container instanceof OkCancelButtonWizardDialog) {
					((OkCancelButtonWizardDialog)container).autoFinish();
				}
			}
		});

		// load the Docker images
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					model.setDockerImages(model.getDockerConnection().getImages(true));

				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}

	private static ModifyListener onFilterImages(final TableViewer dockerImagesTableViewer) {
		return new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				dockerImagesTableViewer.refresh();			
			}
		};
	}

	private static TableViewerColumn addTableViewerColum(final TableViewer tableViewer, final String title,
			final int style, final int alignment, final int width, final CellLabelProvider columnLabelProvider) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, style);
		final TableColumn column = viewerColumn.getColumn();
		if (title != null) {
			column.setText(title);
		}
		column.setAlignment(alignment);
		column.setWidth(width);
		viewerColumn.setLabelProvider(columnLabelProvider);
		return viewerColumn;
	}

	static class ImageIDColumnLabelProvider extends ColumnLabelProvider {

		@Override
		public String getText(final Object element) {
			return ((DockerImageTag) element).getId();
		}
	}

	static class ImageNameColumnLabelProvider extends ColumnLabelProvider {

		@Override
		public String getText(final Object element) {
			return ((DockerImageTag) element).getRepoName();
		}
	}

}
