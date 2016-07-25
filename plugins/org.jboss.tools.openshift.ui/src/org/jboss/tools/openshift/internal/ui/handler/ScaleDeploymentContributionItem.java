/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.IRunningPodHolder;

public class ScaleDeploymentContributionItem extends CompoundContributionItem implements IWorkbenchContribution {
	static final String COMMAND_ID = "org.jboss.tools.openshift.ui.command.deployment.scale";
	static final String DYNAMIC_ITEM_ID = "org.jboss.tools.openshift.ui.command.deployment.dynamic.scale";
    private IServiceLocator fServiceLocator;

    public ScaleDeploymentContributionItem() {}
   
	@Override
	public void initialize(IServiceLocator serviceLocator) {
		fServiceLocator = serviceLocator;
	}

	@Override
	public boolean isVisible() {
		return isRelevant();
	}

    @Override
	public void fill(Menu menu, int index) {
    	if(!isRelevant()) {
    		return;
    	}
        if (index == -1) {
			index = menu.getItemCount();
		}
        Menu m = new Menu(menu);
       	super.fill(m, 0);
       	MenuItem item = new MenuItem(menu, SWT.CASCADE);
       	item.setMenu(m);
       	item.setText("Scale");
    }
    
    @SuppressWarnings("unchecked")
	private boolean isRelevant() {
		ISelectionService service = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = service != null ? service.getSelection() : null;
		if(selection == null || selection.isEmpty()) {
			return false;
		}
		IRunningPodHolder runningPod = UIUtils.getFirstElement(selection, IRunningPodHolder.class);
		if(runningPod == null 
				|| ScaleDeploymentHandler.getServiceWrapperForRunningPod(UIUtils.getFirstElement(selection, IResourceWrapper.class)) == null) {
			return false;
		}
		return true;
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected IContributionItem[] getContributionItems() {
    	if(!isRelevant()) {
			return new IContributionItem[0];
		}
		
		Map mapUp = new HashMap();
		mapUp.put(ScaleDeploymentHandler.REPLICA_DIFF, "1");
		CommandContributionItemParameter pUp = new CommandContributionItemParameter(
				fServiceLocator, DYNAMIC_ITEM_ID + ".up", COMMAND_ID,
				mapUp, OpenShiftImages.TREND_UP, null, null, "Up", null, "Increment the number of deployed replicas by one.", 0, null, true);
		Map mapDown = new HashMap();
		mapDown.put(ScaleDeploymentHandler.REPLICA_DIFF, "-1");
		CommandContributionItemParameter pDown = new CommandContributionItemParameter(
				fServiceLocator,  DYNAMIC_ITEM_ID + ".down", COMMAND_ID, mapDown, 
				OpenShiftImages.TREND_DOWN, null, null, "Down", null, "Increment the number of deployed replicas by one.", 0, null, true);
		CommandContributionItemParameter pTo = new CommandContributionItemParameter(
				fServiceLocator, DYNAMIC_ITEM_ID + ".to", COMMAND_ID,
				new HashMap(), null, null, null, "To...", null, "Scale the number of deployed replicas to a specific value.", 0, null, true);

		return new IContributionItem[] {
				new CommandContributionItem(pUp),
				new CommandContributionItem(pDown),
				new CommandContributionItem(pTo)};
 	}

}
