/*******************************************************************************
 * Copyright (c) 2014-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.reddeer.swt.api.Shell;
import org.jboss.reddeer.swt.condition.ShellIsAvailable;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.combo.DefaultCombo;
import org.jboss.reddeer.swt.impl.link.DefaultLink;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.ConnectionUtils;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.junit.Test;

/**
 * Test opening new connection dialog via tool item and context menu.
 * 
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 *
 */

@OpenPerspective(value=JBossPerspective.class)
public class OpenNewConnectionWizardTest {
	
	OpenShiftExplorerView explorer = new OpenShiftExplorerView();
	
	@Test
	public void shouldOpenConnectionWizardViaLinkTest() {
		// given
		ConnectionUtils.clearAll();
		explorer.open();

		// when
		new DefaultLink(OpenShiftLabel.TextLabels.CREATE_CONNECTION).click();

		// then
		verifyNewConnectionWizardIsOpened();
	}
	
	@Test
	public void shouldOpenConnectionWizardViaToolItemTest() {
		// given
		explorer.open();
		
		// when
		new DefaultToolItem(OpenShiftLabel.Others.CONNECT_TOOL_ITEM).click();

		// then
		verifyNewConnectionWizardIsOpened();
	}
	
	private void verifyNewConnectionWizardIsOpened() {
		Shell connectionShell = new DefaultShell(OpenShiftLabel.Shell.NEW_CONNECTION);
		new DefaultCombo(OpenShiftLabel.TextLabels.NEW_CONNECTION);

		new CancelButton().click();

		new WaitWhile(new ShellIsAvailable(connectionShell));
	}
}
