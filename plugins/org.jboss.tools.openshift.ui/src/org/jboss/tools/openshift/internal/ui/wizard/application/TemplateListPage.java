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
package org.jboss.tools.openshift.internal.ui.wizard.application;

import org.apache.commons.lang.StringUtils;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.restclient.model.template.ITemplate;

/**
 * A page that offers a list templates to a user
 * @author jeff.cantrill
 *
 */
public class TemplateListPage  extends AbstractOpenShiftWizardPage  {

	public ITemplateListPageModel model;
	
	public TemplateListPage(IWizard wizard, ITemplateListPageModel model) {
		super("Select template", "Select a template that defines the resources for an application", "templateList", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(1)
			.margins(10, 6)
			.spacing(2, 2)
			.applyTo(parent);
		
		Composite applicationTemplatesTreeComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.align(SWT.FILL, SWT.FILL)
				.grab(true, true)
				.applyTo(applicationTemplatesTreeComposite);
		GridLayoutFactory.fillDefaults().spacing(2, 2).applyTo(applicationTemplatesTreeComposite);
		
		// the list of templates
		TreeViewer viewer = createTemplatesViewer(applicationTemplatesTreeComposite, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.hint(400, 180)
				.applyTo(viewer.getControl());
		IObservableValue viewObservable = ViewerProperties.singleSelection().observe(viewer);
		IObservableValue modelObservable = BeanProperties.value(
				ITemplateListPageModel.PROPERTY_TEMPLATE).observe(model);
		ValueBindingBuilder
			.bind(viewObservable)
			.to(modelObservable)
			.in(dbc);
		viewer.setSelection(null);
		
		//details
		final Group detailsContainer = new Group(applicationTemplatesTreeComposite, SWT.NONE);
		detailsContainer.setText("Details");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL)
				.hint(SWT.DEFAULT, 106)
				.applyTo(detailsContainer);
		
		new TemplateDetailViews(modelObservable, null, detailsContainer, dbc).createControls();;
	}

	private TreeViewer createTemplatesViewer(Composite parent, DataBindingContext dbc) {
		TreeViewer viewer = 	new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(element instanceof ITemplate) {
					ITemplate template = (ITemplate) element;
					StyledString text = new StyledString(template.getName());
					if(template.isAnnotatedWith("tags")) {
						String [] tags = template.getAnnotation("tags").split(",");
						text.append(NLS.bind(" ({0})", StringUtils.join(tags, ", ")), StyledString.DECORATIONS_STYLER);
					}
					cell.setText(text.toString());
					cell.setStyleRanges(text.getStyleRanges());
				}
				super.update(cell);
			}
		});
		viewer.setContentProvider(new TemplateListPageTreeContentProvider());
		viewer.setInput(model.getTemplates());
		return viewer;
	}
	
	private class TemplateListPageTreeContentProvider implements ITreeContentProvider{
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		@Override
		public void dispose() {
		}
		
		@Override
		public boolean hasChildren(Object node) {
			return false;
		}
		
		@Override
		public Object getParent(Object node) {
			return null;
		}
		
		@Override
		public Object[] getElements(Object node) {
			return model.getTemplates().toArray();
		}
		
		@Override
		public Object[] getChildren(Object node) {
			return null;
		}
	}
}
