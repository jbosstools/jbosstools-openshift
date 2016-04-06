/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;

import com.openshift.restclient.model.IBuildConfig;

/**
 * @author Jeff Maury
 */
public class BuildConfigDetailViews extends AbstractStackedDetailViews {

	private final IDetailView templateView = new TemplateDetailView();

	public BuildConfigDetailViews(IObservableValue serviceObservable, Composite parent, DataBindingContext dbc) {
		super(serviceObservable, null, parent, dbc);
	}

	@Override
	protected void createViewControls(Composite parent, Object context, DataBindingContext dbc) {
		templateView.createControls(parent, context, dbc);
		emptyView.createControls(parent, context, dbc);
	}

	@Override
	protected IDetailView[] getDetailViews() {
		return new IDetailView[] { templateView, emptyView };
	}

	private class TemplateDetailView extends EmptyView {

		private StyledText nameText;
		private StyledText namespaceText;
		private StyledText labelsText;
		private StyledText sourceURIText;
		private StyledText outputRepositoryNameText;

		@Override
		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.numColumns(2).margins(8, 2).spacing(6, 2).applyTo(container);
			this.nameText = createLabeledValue("Name:", container);
			this.namespaceText = createLabeledValue("Namespace:", container);
			this.labelsText = createLabeledValue("Labels:", container);
			this.sourceURIText = createLabeledValue("Source URI:", container);
			this.outputRepositoryNameText = createLabeledValue("Output repository:", container);

			return container;
		}

		FocusListener fl = new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
			}

			@Override
			public void focusLost(FocusEvent e) {
				if(e.getSource() instanceof StyledText) {
					((StyledText)e.getSource()).setSelection(0, 0);
				}
			}

		};

		private StyledText createLabeledValue(String labelText, Composite container) {
			Label label = new Label(container, SWT.NONE);
			label.setText(labelText);
			GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(label);
			StyledText styledText = new StyledText(container, SWT.READ_ONLY);
			styledText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(styledText);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(styledText);
			styledText.addFocusListener(fl);
			return styledText;
		}

		@Override
		public void onVisible(IObservableValue serviceObservable, DataBindingContext dbc) {
			Object value = serviceObservable.getValue();
			if (!(value instanceof IBuildConfig)) {
				return;
			}
			IBuildConfig buildConfig = (IBuildConfig) value;

			nameText.setText(buildConfig.getName());
			namespaceText.setText(buildConfig.getNamespace());
			String labels = org.jboss.tools.openshift.common.core.utils.StringUtils.toString(buildConfig.getLabels());
			labels = org.apache.commons.lang.StringUtils.defaultString(labels); //replaces null by empty string
			labelsText.setText(labels);
			sourceURIText.setText(buildConfig.getSourceURI());
			outputRepositoryNameText.setText(buildConfig.getOutputRepositoryName());
		}

		@Override
		public void onInVisible(IObservableValue templateObservable, DataBindingContext dbc) {
		}

		@Override
		public boolean isViewFor(Object object) {
			return object instanceof IBuildConfig;
		}
	}
}
