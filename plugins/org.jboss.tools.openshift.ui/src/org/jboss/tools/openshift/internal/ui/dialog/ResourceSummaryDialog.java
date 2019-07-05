/*******************************************************************************
 * Copyright (c) 2015-2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.dialog;

import java.util.Collection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jboss.tools.common.log.StatusFactory;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.server.ResourceDetailViews;

import com.openshift.restclient.model.IResource;

/**
 * A dialog to display info about a list of resources
 * @author jeff.cantrill
 */
public class ResourceSummaryDialog extends TitleAreaDialog {

	private static final int RESOURCES_VIEW_MIN_WIDTH = 700;
	private Collection<IResource> resources;
	private IStatus status;
	private String dialogTitle;
	private IStyledLabelProvider labelProvider;
	private ITreeContentProvider contentProvider;

	public ResourceSummaryDialog(Shell parentShell, Collection<IResource> resources, String dialogTitle,
			String message) {
		this(parentShell, resources, dialogTitle, StatusFactory.getInstance(IStatus.OK, OpenShiftUIActivator.PLUGIN_ID, message));
	}

	public ResourceSummaryDialog(Shell parentShell, Collection<IResource> resources, String dialogTitle,
			IStatus status) {
		this(parentShell, resources, dialogTitle, status, new ResourceSummaryLabelProvider(), new ResourceSummaryContentProvider());
	}

	public ResourceSummaryDialog(Shell parentShell, Collection<IResource> resources, String dialogTitle,
			String message, IStyledLabelProvider labelProvider, ITreeContentProvider contentProvider) {
		this(parentShell, resources, dialogTitle, 
				StatusFactory.getInstance(IStatus.OK, OpenShiftUIActivator.PLUGIN_ID, message),
				labelProvider, contentProvider);
	}

	public ResourceSummaryDialog(Shell parentShell, Collection<IResource> resources, String dialogTitle, IStatus status,
			IStyledLabelProvider labelProvider, ITreeContentProvider contetProvider) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.resources = resources;
		this.status = status;
		this.labelProvider = labelProvider;
		this.contentProvider = contetProvider;
		setHelpAvailable(false);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		setupDialog(parent);
		return control;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.None);
		GridLayoutFactory.fillDefaults()
			.margins(10,10)
			.applyTo(composite);
		GridDataFactory.fillDefaults()
			.grab(true, true)
			.applyTo(composite);
		
		Label newResourceLabel = new Label(composite, SWT.None);
		newResourceLabel.setText("New Resources Created:");
		GridDataFactory.fillDefaults()
			.indent(0, 10).align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(newResourceLabel);

		createResourcesView(composite);

		// hook for subclassing
		createAreaAfterResourceSummary(composite);

		Label buttonsSeparator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.TOP).grab(true, false)
			.applyTo(buttonsSeparator);

		return composite;
	}

	private void createResourcesView(Composite parent) {
		SashForm resourceControlsContainer = new SashForm(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(false, false).hint(RESOURCES_VIEW_MIN_WIDTH, SWT.DEFAULT)
			.applyTo(resourceControlsContainer);
		UIUtils.adjustWidthHintOnResize(resourceControlsContainer);
		
		StructuredViewer treeViewer = createTreeViewer(resourceControlsContainer);
		createResourceDetails(treeViewer, resourceControlsContainer);

		resourceControlsContainer.setWeights(new int[] { 1, 1 });
	}

	private StructuredViewer createTreeViewer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.applyTo(container);

		TreeColumnLayout treeLayout = new TreeColumnLayout();
		container.setLayout(treeLayout);
		final TreeViewer viewer = new TreeViewer(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);		
		viewer.setInput(resources);

		return viewer;
	}


	private void createResourceDetails(StructuredViewer viewer, Composite parent) {
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		ExpandableComposite expandable = new ExpandableComposite(scrolledComposite, SWT.None);
		scrolledComposite.setContent(expandable);
		expandable.setText("Resource Details");
		expandable.setExpanded(true);
		expandable.setLayout(new FillLayout());
		Composite detailsContainer = new Composite(expandable, SWT.NONE);
		expandable.setClient(detailsContainer);
		expandable.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				parent.update();
				parent.layout(true);
			}
		});

		DataBindingContext dbc = new DataBindingContext();
		IViewerObservableValue<Object> selectedResource = ViewerProperties.singlePostSelection().observe(viewer);
		new ResourceDetailViews(selectedResource, detailsContainer, dbc).createControls();
	}

	/**
	 * Hook to allow sublcasses to add content
	 * @param  parent   the dialog area to which the tree viewer is added
	 */
	protected void createAreaAfterResourceSummary(Composite parent) {
	}

	private void setupDialog(Composite parent) {
		parent.getShell().setText(dialogTitle);
		setMessage(status.getMessage(), status.getSeverity());
		setTitleImage(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM_IMG);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

}
