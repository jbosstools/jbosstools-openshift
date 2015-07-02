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
package org.jboss.tools.openshift.internal.common.ui.utils;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.ToolTip;
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
		viewer.setComparer(new EqualityComparer());
		this.tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
	}

	public TableViewerBuilder contentProvider(IStructuredContentProvider contentProvider) {
		viewer.setContentProvider(contentProvider);
		return this;
	}
	
	public TableViewerBuilder comparer(IElementComparer sorter) {
		viewer.setComparer(sorter);
		return this;
	}
	
	public TableViewerBuilder sorter(ViewerSorter sorter) {
		viewer.setSorter(sorter);
		return this;
	}

	public <E>ColumnBuilder<E> column(String name) {
		return new ColumnBuilder<E>().name(name);
	}
	
	public <E>ColumnBuilder<E> column(IColumnLabelProvider<E> columnLabelProvider) {
		return new ColumnBuilder<E>().labelProvider(columnLabelProvider);
	}

	public TableViewer buildViewer() {
		return viewer;
	}
	
	public class ColumnBuilder<E> {
		
		private int alignement;
		private IColumnLabelProvider<E> columnLabelProvider;
		private ICellToolTipProvider<E> cellToolTipProvider;
		private String name;
		private int weight;
		private int minWidth = ColumnWeightData.MINIMUM_WIDTH;

		private ColumnBuilder() {
		}
		
		public ColumnBuilder<E> labelProvider(IColumnLabelProvider<E> labelProvider) {
			this.columnLabelProvider = labelProvider;
			return this;
		}

		public ColumnBuilder<E> cellToolTipProvider(ICellToolTipProvider<E> cellToolTipProvider) {
			this.cellToolTipProvider = cellToolTipProvider;
			return this;
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
		
		public ColumnBuilder<E> minWidth(int minWidth) {
			this.minWidth = minWidth;
			return this;
		}

		public TableViewerBuilder buildColumn() {
			if(cellToolTipProvider != null) {
				ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE); 
			}
			TableViewerColumn column = new TableViewerColumn(viewer, alignement);
			column.getColumn().setText(name);
			column.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(ViewerCell cell) {
					@SuppressWarnings("unchecked")
					String cellValue = columnLabelProvider.getValue((E) cell.getElement());
					cell.setText(cellValue);
				}
				
				@SuppressWarnings("unchecked")
				@Override
				public String getToolTipText(Object object) {
					if(cellToolTipProvider != null) {
						return cellToolTipProvider.getToolTipText((E) object);
					}
					return super.getToolTipText(object);
				};
				
				@SuppressWarnings("unchecked")
				@Override
				public int getToolTipDisplayDelayTime(Object element) {
					if(cellToolTipProvider != null) {
						return cellToolTipProvider.getToolTipDisplayDelayTime((E) element);
					}
					return super.getToolTipDisplayDelayTime(element);
				}
				
			});
			tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(weight, minWidth, true));
			return TableViewerBuilder.this;
		}
	}

	public static interface IColumnLabelProvider<E> {
		public String getValue(E e);
	}
	
	/**
	 * Add tooltips to a cell on hoveover
	 * @link {@link CellLabelProvider}
	 * @author jeff.cantrill
	 *
	 * @param <T>
	 */
	public static interface ICellToolTipProvider<T> {
		
		String getToolTipText(T object);
		
		int getToolTipDisplayDelayTime(T object);
	}

	/**
	 * Viewer element comparer based on #equals(). The default implementation in
	 * CheckboxTableViewer compares elements based on instance identity.
	 * <p>
	 * We need this since the available cartridges (item listed in the viewer)
	 * are not the same instance as the ones in the embedded application (items
	 * to check in the viewer).
	 */
	public static class EqualityComparer implements IElementComparer {

		@Override
		public boolean equals(Object thisObject, Object thatObject) {
			if (thisObject == null) {
				return thatObject != null;
			}

			if (thatObject == null) {
				return false;
			}

			return thisObject.equals(thatObject);
		}

		@Override
		public int hashCode(Object element) {
			return element.hashCode();
		}
	}

}
