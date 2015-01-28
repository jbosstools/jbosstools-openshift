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
package org.jboss.tools.openshift.internal.ui.wizard.deployment;

import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.kube.images.DockerImageDescriptor;

/**
 * @author Jeff Cantrill
 */
public class DeploymentWizardPage extends AbstractOpenShiftWizardPage {

	private DeploymentWizardPageModel pageModel;

	public DeploymentWizardPage(IWizard wizard, DeploymentWizardContext context) {
		super("New OpenShift Deployment", "Create a new deployment", "New application deployment", wizard);
		this.pageModel = new DeploymentWizardPageModel(context);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.numColumns(1)
				.margins(10, 6)
				.spacing(2, 2)
				.applyTo(parent);

		Label lblBaseImage = new Label(parent, SWT.None);
		lblBaseImage.setText("Select a base image to support your application:");
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(lblBaseImage);

		Composite baseImagesTreeComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL)
				.grab(true, true)
				.applyTo(baseImagesTreeComposite);
		GridLayoutFactory.fillDefaults().spacing(2, 2).applyTo(baseImagesTreeComposite);

		// base images tree
		TreeViewer viewer = createBaseImagesViewer(parent, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.hint(400, 180)
				.applyTo(viewer.getControl());
	}

	private TreeViewer createBaseImagesViewer(Composite parent, DataBindingContext dbc) {
		TreeViewer viewer =
				new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				StyledString text = new StyledString();
				if (element instanceof DockerImageDescriptor) {
					DockerImageDescriptor descriptor = (DockerImageDescriptor) element;
					text.append(descriptor.getDescription());
					text.append(" ", StyledString.DECORATIONS_STYLER);
					text.append(descriptor.getImageUri().getUriWithoutHost());
				}else{
					text.append(element.toString());
				}
				cell.setText(text.toString());
				super.update(cell);
			}
		});
		viewer.setContentProvider(new ITreeContentProvider() {

			List<DockerImageDescriptor> images = pageModel.getBaseImages();

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return images.toArray();
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		});
		viewer.setInput(pageModel.getBaseImages());

		// bind selection
		IObservableValue viewObservable = ViewerProperties.singleSelection().observe(viewer);
		IObservableValue modelObservable = BeanProperties.value(
				DeploymentWizardPageModel.PROPERTY_SELECTED_IMAGE).observe(pageModel);
		ValueBindingBuilder
				.bind(viewObservable)
				.to(modelObservable)
				.in(dbc);
		return viewer;
	}

}
