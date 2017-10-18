/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.utils;

import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.cdk.reddeer.server.adapter.CDKServerAdapterType;


/**
 * Utility class for CDK reddeer test plugin
 * @author odockal
 *
 */
public class CDKUtils {

	public static CDKServerAdapterType getCDKServerType(String serverType) {
		for (CDKServerAdapterType type : CDKServerAdapterType.values()) {
			if (type.serverType().equals(serverType)) {
				return type;
			}
		}
		return CDKServerAdapterType.NO_CDK;
	}

	public static String getServerTypeIdFromItem(TreeItem item) {
		Object itemData = Display.syncExec(new ResultRunnable<Object>() {
			@Override
			public Object run() {
				return item.getSWTWidget().getData();
			}
		});
		if (IServer.class.isInstance(itemData)) {
			return ((IServer)itemData).getServerType().getId();
		}
		return "";
	}
	
}
