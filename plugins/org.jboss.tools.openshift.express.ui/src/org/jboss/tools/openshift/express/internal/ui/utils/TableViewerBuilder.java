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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * @author Andre Dietisheim
 */
public class TableViewerBuilder {

	private TableViewer viewer;
	private TableColumnLayout tableLayout;
	
	public TableViewerBuilder(Table table, Composite tableContainer) {
		this(new TableViewer(table), tableContainer);
	}

	public TableViewerBuilder(TableViewer viewer, Composite tableContainer) {
		this.viewer = viewer;
		this.tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
	}

	public TableViewerBuilder contentProvider(IStructuredContentProvider contentProvider) {
		viewer.setContentProvider(contentProvider);
		return this;
	}
	
	public <V> ColumnBuilder<V> column(ICellValueProvider<V> valueProvider) {
		return new ColumnBuilder<V>(valueProvider);
	}
	
	public TableViewer buildViewer() {
		return viewer;
	}
	
	public class ColumnBuilder<E> {
		
		private int alignement;
		private ICellValueProvider<E> cellValueProvider;
		private String name;
		private int weight;

		private ColumnBuilder(ICellValueProvider<E> valueProvider) {
			this.cellValueProvider = valueProvider;
		}

		public ColumnBuilder<E> align(int alignement) {
			this.alignement = alignement;
			return this;
		}

		public ColumnBuilder<E> name(String name) {
			this.name = name;
			return this;
		}
		
		public ColumnBuilder<E> weight(int weight) {
			this.weight = weight;
			return this;
		}
		
		public TableViewerBuilder buildColumn() {
			TableViewerColumn column = new TableViewerColumn(viewer, alignement);
			column.getColumn().setText(name);
			column.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(ViewerCell cell) {
					@SuppressWarnings("unchecked")
					String cellValue = cellValueProvider.getValue((E) cell.getElement());
					cell.setText(cellValue);
				}
			});
			tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
			return TableViewerBuilder.this;
		}
	}

	public static interface ICellValueProvider<E> {
		public String getValue(E e);
	}
	
}
