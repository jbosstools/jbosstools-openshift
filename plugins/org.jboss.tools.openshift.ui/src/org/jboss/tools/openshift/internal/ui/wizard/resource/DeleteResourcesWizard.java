/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.ui.jobs.DisableAllWidgetsJob;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory.KeyValueFilter;
import org.jboss.tools.openshift.internal.common.core.util.KeyValueFilterFactory.KeyValueFilter.Range;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.comparators.ResourceKindAndNameViewerComparator;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;

/**
 * A wizard that allows to delete resources (that a user selects) from a given
 * project
 * 
 * @author Andre Dietisheim
 */
public class DeleteResourcesWizard extends AbstractOpenShiftWizard<DeleteResourcesWizardModel> {

	private Connection connection;
	private String projectName;
	private DeleteResourcesPage deleteResourcesPage;

	public DeleteResourcesWizard(Connection connection, String projectName) {
		super("Delete OpenShift Resources", new DeleteResourcesWizardModel(connection, projectName));
		this.connection = connection;
		this.projectName = projectName;
	}

	@Override
	public void addPages() {
		this.deleteResourcesPage = new DeleteResourcesPage(connection, projectName, this);
		addPage(deleteResourcesPage);
	}

	@Override
	public boolean performFinish() {
		deleteResourcesPage.deleteResources();
		return true;
	}

	public static class DeleteResourcesPage extends AbstractOpenShiftWizardPage {

		private DeleteResourcesWizardModel model;

		public DeleteResourcesPage(Connection connection, String projectName, IWizard wizard) {
			super(NLS.bind("Delete Resources in project {0}", projectName),
					"Select the resources that you want to delete from OpenShift", "deleteResources", wizard);
			this.model = new DeleteResourcesWizardModel(connection, projectName);
		}

		@Override
		protected void doCreateControls(Composite parent, DataBindingContext dbc) {
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);
			createFilterControls(parent, dbc);
			createResourcesControl(parent, dbc);
		}

		@SuppressWarnings("unchecked")
		private void createFilterControls(Composite parent, DataBindingContext dbc) {
			// filter text
			Label filterLabel = new Label(parent, SWT.None);
			filterLabel.setText("Label Filter:");
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(filterLabel);
			Text filterText = UIUtils.createSearchText(parent);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(filterText);
			Binding filterTextBinding = ValueBindingBuilder
					.bind(WidgetProperties.text(SWT.Modify).observe(filterText))
						.converting(new Converter(String.class, KeyValueFilter.class) {

							@Override
							public Object convert(Object fromObject) {
								if (!(fromObject instanceof String)) {
									return null;
								}
								try {
									String filterText = (String) fromObject;
									if (StringUtils.isEmpty(filterText)) {
										return new KeyValueFilter();
									}
									List<KeyValueFilter> filters = KeyValueFilterFactory.create(filterText);
									return filters.stream().findFirst().orElse(null);
								} catch (PatternSyntaxException e) {
									return null;
								}
							}
						})
						.validatingAfterConvert(value -> {
							if (value == null) {
								return ValidationStatus.error("Invalid filter expression provided. "
										+ "Please either use plain text or conform to regular expression rules.");
							}
							return ValidationStatus.ok();
						})
					.to(BeanProperties.value(DeleteResourcesWizardModel.PROP_LABEL_FILTER).observe(model))
					.in(dbc);
			ControlDecorationSupport.create(filterTextBinding, SWT.LEFT | SWT.TOP, null);

			// filler
			new Label(parent, SWT.NONE);
			// explanatory label
			Label explanatoryLabel = new Label(parent, SWT.NONE | SWT.WRAP);
			explanatoryLabel.setText("You can filter resources based on their labels.\n"
					+ "Filter expressions take fhe form <value substring> or <key substring>=<value substring>. "
					+ "Key- and value-expressions can be regular expressions (ex. \".*\").");
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.LEFT, SWT.CENTER).applyTo(explanatoryLabel);
		}

		@SuppressWarnings("unchecked")
		private void createResourcesControl(Composite parent, DataBindingContext dbc) {
			Composite resourcesContainer = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, SWT.DEFAULT).align(SWT.FILL, SWT.FILL).grab(true, true)
					.applyTo(resourcesContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(resourcesContainer);
			// resources table
			Composite tableContainer = new Composite(resourcesContainer, SWT.NONE);
			IObservableValue<KeyValueFilter> labelFilter = BeanProperties
					.value(DeleteResourcesWizardModel.PROP_LABEL_FILTER).observe(model);
			TableViewer resourcesViewer = createResourcesTable(labelFilter, tableContainer);
			GridDataFactory.fillDefaults().span(1, 3).align(SWT.FILL, SWT.CENTER).hint(SWT.DEFAULT, 500)
					.grab(true, false).applyTo(tableContainer);
			IObservableList<IParameter> allResourcesObservable = BeanProperties
					.list(DeleteResourcesWizardModel.PROP_ALL_RESOURCES).observe(model);
			resourcesViewer.setInput(allResourcesObservable);
			IObservableList<IResource> selectedResources = BeanProperties
					.list(DeleteResourcesWizardModel.PROP_SELECTED_RESOURCES).observe(model);
			dbc.bindList(ViewerProperties.multipleSelection().observe(resourcesViewer), selectedResources);
			dbc.addValidationStatusProvider(new MultiValidator() {

				@Override
				protected IStatus validate() {
					if (selectedResources.isEmpty()) {
						return ValidationStatus.cancel("Please select the resources that should be deleted.");
					} else {
						return ValidationStatus.ok();
					}
				}
			});

			// select all button
			Button selectAllButton = new Button(resourcesContainer, SWT.PUSH);
			selectAllButton.setText("Select All");
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
					.hint(UIUtils.getDefaultButtonWidth(selectAllButton), SWT.DEFAULT).applyTo(selectAllButton);
			selectAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(
					event -> resourcesViewer.setSelection(new StructuredSelection(model.getAllResources()))));
			// deselect all button
			Button deselectAllButton = new Button(resourcesContainer, SWT.PUSH);
			deselectAllButton.setText("Deselect All");
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
					.hint(UIUtils.getDefaultButtonWidth(deselectAllButton), SWT.DEFAULT).applyTo(deselectAllButton);
			deselectAllButton.addSelectionListener(SelectionListener
					.widgetSelectedAdapter(event -> resourcesViewer.setSelection(new StructuredSelection())));
			// filler
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, true)
					.applyTo(new Label(resourcesContainer, SWT.NONE));

		}

		protected TableViewer createResourcesTable(final IObservableValue<KeyValueFilter> labelFilter,
				final Composite tableContainer) {
			Table table = new Table(tableContainer,
					SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			TableViewer resourcesViewer = new TableViewerBuilder(table, tableContainer)
					.column(resource -> ((IResource) resource).getName())
						.name("Name")
						.align(SWT.LEFT)
						.weight(2)
						.minWidth(200).buildColumn()
					.column(resource -> ((IResource) resource).getKind())
						.name("Type")
						.align(SWT.LEFT)
						.weight(1)
						.minWidth(100)
						.buildColumn()
					.column(new LabelsCellLabelProvider(labelFilter))
						.name("Labels")
						.align(SWT.LEFT)
						.weight(3)
						.minWidth(300)
						.buildColumn()
					.buildViewer();
			resourcesViewer.setContentProvider(new ObservableListContentProvider());
			resourcesViewer.setComparator(new ResourceKindAndNameViewerComparator());
			resourcesViewer.setFilters(new ResourceLabelFilter(labelFilter));
			return resourcesViewer;
		}

		@Override
		protected void onPageActivated(DataBindingContext dbc) {
			loadResources(dbc);
		}

		private void loadResources(DataBindingContext dbc) {
			DelegatingProgressMonitor delegatingMonitor = new DelegatingProgressMonitor();
			Job job = new JobChainBuilder(new DisableAllWidgetsJob(true, (Composite) getControl(), null))
					.runWhenDone(new Job("Retrieving all resources from server...") {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							delegatingMonitor.add(monitor);
							model.loadResources(delegatingMonitor);
							return Status.OK_STATUS;
						}
					}).runWhenDone(new DisableAllWidgetsJob(false, (Composite) getControl(), null)).build();
			try {
				WizardUtils.runInWizard(job, delegatingMonitor, getContainer(), dbc);
			} catch (InvocationTargetException | InterruptedException e) {
				// ignore
			}

		}

		public void deleteResources() {
			model.deleteSelectedResources();
		}

		private static final class LabelsCellLabelProvider extends StyledCellLabelProvider {

			private static final String KEY_VALUE_DELIMITER = "=";
			private static final String LABELS_DELIMITER = ", ";

			private IObservableValue<KeyValueFilter> filterObservable;

			public LabelsCellLabelProvider(IObservableValue<KeyValueFilter> filter) {
				this.filterObservable = filter;
			}

			@Override
			public void update(ViewerCell cell) {
				KeyValueFilter filter = filterObservable.getValue();
				IResource resource = (IResource) cell.getElement();
				Map<String, String> filteredLabels = ResourceUtils.getMatchingLabels(filter, resource);
				createCellLabel(filter, filteredLabels, cell);
				((Table) cell.getControl()).getColumn(cell.getColumnIndex()).pack();
				((Table) cell.getControl()).redraw();
			}

			private void createCellLabel(KeyValueFilter filter, Map<String, String> labels, ViewerCell cell) {
				List<StyleRange> styles = new ArrayList<>();
				StringBuilder builder = new StringBuilder();
				labels.entrySet().forEach(entry -> {

					if (builder.length() > 0) {
						builder.append(LABELS_DELIMITER);
					}
					createKeyLabel(filter, styles, builder, entry);
					createValueLabel(filter, styles, builder, entry);
				});

				cell.setText(builder.toString());
				cell.setStyleRanges(styles.toArray(new StyleRange[styles.size()]));
			}

			private void createValueLabel(KeyValueFilter filter, List<StyleRange> styles, StringBuilder builder,
					Entry<String, String> entry) {
				builder.append(KEY_VALUE_DELIMITER);
				if (filter != null) {
					Range range = filter.getMatchingRangeForValue(entry.getValue());
					addMatchStyleRange(range, builder.length(), styles);
				}
				builder.append(entry.getValue());
			}

			private void createKeyLabel(KeyValueFilter filter, List<StyleRange> styles, StringBuilder builder,
					Entry<String, String> entry) {
				if (filter != null) {
					Range range = filter.getMatchingRangeForKey(entry.getKey());
					addMatchStyleRange(range, builder.length(), styles);
				}
				builder.append(entry.getKey());
			}

			private void addMatchStyleRange(Range range, int startPos, List<StyleRange> styles) {
				if (range == null) {
					return;
				}
				styles.add(StyledTextUtils.createSelectedStyle(startPos + range.start, range.length));
			}
		}

	}

}
