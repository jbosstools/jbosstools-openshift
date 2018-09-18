/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.jboss.tools.cdk.reddeer.core.runtime.Runtime;

/**
 * Moved and duplicated from org.jboss.tools.as.ui.bot.itests.reddeer
 *
 */
public class SearchingForRuntimesDialog extends DefaultShell {
	
	public static final String DIALOG_TITLE = "Searching for runtimes...";
	
	public SearchingForRuntimesDialog() {
		super(DIALOG_TITLE);
	}
	
	/**
	 * We get columns names and their indexes.
	 * 
	 * To ensure compatibility with different versions of JBDS where the table
	 * has different order of columns.
	 */
	private Map<String, Integer> columnsNames = Display.syncExec(new ResultRunnable<Map<String, Integer>>(){
		@Override
		public Map<String, Integer> run() {
			Map<String, Integer> columnsNames = new TreeMap<String, Integer>();
			// get real values
			Tree tree = new DefaultTree().getSWTWidget();
			TreeColumn[] columns = tree.getColumns();
			for(int i=0;i<columns.length;i++) {
				columnsNames.put(columns[i].getText(), i);
			}
			return columnsNames;
		}
	});
	
	/**
	 * Returns text from cell in column with given name
	 * @param row from which the cell's text will be returned
	 * @param columnName
	 * @return text from the cell
	 */
	private String getText(TreeItem row, String columnName) {
		int columnIndex = columnsNames.get(columnName).intValue();
		return row.getCell( columnIndex );
	}
	
	public List<Runtime> getRuntimes(){
		List<Runtime> runtimes = new ArrayList<Runtime>();
		for (TreeItem treeItem : getRuntimesTreeItems()) {
			Runtime runtime = new Runtime();
			runtime.setName( getText(treeItem, "Name") );
			runtime.setVersion( getText(treeItem, "Version") );
			runtime.setType( getText(treeItem, "Type") );
			runtime.setLocation( getText(treeItem, "Location") );
			runtimes.add(runtime);
		}
		return runtimes;
	}
	
	public void ok(){
		new PushButton(this, "OK").click();
		new WaitWhile(new ShellIsAvailable(this));
	}
	
	public void cancel(){
		new PushButton(this, "Cancel").click();
		new WaitWhile(new ShellIsAvailable(this));
	}

	public void hideAlreadyCreatedRuntimes() {
		new CheckBox(this, "Hide already created runtimes").toggle(true);
	}
	
	public void deselect(String runtimeName){
		for (TreeItem treeItem : getRuntimesTreeItems()) {
			if (treeItem.getCell(0).equals(runtimeName)){
				treeItem.setChecked(false);
			}
		}
	}
	
	private List<TreeItem> getRuntimesTreeItems(){
		return new DefaultTree(this).getAllItems();
	}
}
