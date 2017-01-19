/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.swt.api.Table;

/**
 * Wait condition waiting till specified table is active.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class TableIsEnabled extends AbstractWaitCondition {

	private Table table;
	
	public TableIsEnabled(Table table) {
		this.table = table;
	}

	@Override
	public boolean test() {
		return table.isEnabled();
	}

	@Override
	public String description() {
		return "table is enabled";
	}
	
	
}
