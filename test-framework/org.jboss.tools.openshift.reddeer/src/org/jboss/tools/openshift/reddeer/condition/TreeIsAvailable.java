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
import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.swt.impl.tree.DefaultTree;

public class TreeIsAvailable extends AbstractWaitCondition {

	public TreeIsAvailable() { }

	@Override
	public boolean test() {
		try {
			new DefaultTree();
			return true;
		} catch (RedDeerException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return "Tree with index 0 is available";
	}
}
