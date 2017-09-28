/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.core.model;

import java.util.List;

public interface IAccountModel {

	void addCluster(ICluster cluster);
	List<ICluster> getClusters();
	void removeCluster(ICluster cluster);
	ICluster createCluster(String id);

	void save();
	
	void addListener(IAccountModelListener listener);
	void removeListener(IAccountModelListener listener);
}
