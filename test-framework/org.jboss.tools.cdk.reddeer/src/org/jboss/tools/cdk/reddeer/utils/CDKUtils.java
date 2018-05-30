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

import java.util.Arrays;
import java.util.List;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.junit.screenshot.CaptureScreenshotException;
import org.eclipse.reddeer.junit.screenshot.ScreenshotCapturer;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.cdk.reddeer.core.enums.CDKServerAdapterType;


/**
 * Utility class for CDK reddeer test plugin
 * @author odockal
 *
 */
public final class CDKUtils {

	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win"); 
	
	public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux"); 
	
	private static final Logger log = Logger.getLogger(CDKUtils.class);

	private CDKUtils() {}
	
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
	
	public static boolean isCDKServer(TreeItem item) {
		String type = CDKUtils.getServerTypeIdFromItem(item);
		log.info("Server type id is " + type);
		return Arrays.stream(CDKServerAdapterType.values()).anyMatch(e -> e.serverType().equals(type));
	}
	
	
	public static void deleteAllCDKServerAdapters() {
		for (Server server : getAllServers()) {
			log.info("Found server with name " + server.getLabel().getName());
			if (CDKUtils.isCDKServer(server.getTreeItem())) {
				log.info("Deleting server...");
				server.delete(true);
			}
		}
	}
	
	public static List<Server> getAllServers() {
		log.info("Collecting all server adapters");
		ServersView2 view = new ServersView2();
		view.open();
		return view.getServers();
	}
	
	
	public static void captureScreenshot(String name) {
		try {
			ScreenshotCapturer.getInstance().captureScreenshot(name);
		} catch (CaptureScreenshotException e) {
			log.error("Could not capture screenshot for " + name);
		}		
	}
	
}
