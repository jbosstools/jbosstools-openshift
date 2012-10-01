package org.jboss.tools.openshift.ui.bot.util;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.jboss.tools.ui.bot.ext.view.ViewBase;

/**
 * 
 * @author sbunciak
 *
 */
public class OpenShiftExplorerView extends ViewBase {

	public OpenShiftExplorerView() {
		super();
		this.viewObject = OpenShiftUI.Explorer.iView;
		show();

	}
	
	public SWTBotToolbarButton getConnectionToolButton() {
		return this.getToolbarButtonWitTooltip(OpenShiftUI.Labels.CONNECT_TO_OPENSHIFT);
	}
	
}
