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
package org.jboss.tools.openshift.ui.bot.test.common;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.core.condition.ShellWithTextIsActive;
import org.jboss.reddeer.jface.wizard.WizardDialog;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.workbench.impl.shell.WorkbenchShell;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage.Direction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author adietish@redhat.com
 */
@RunWith(RedDeerSuite.class)
public class AbstractOpenShiftWizardPageTest {

	private OpenShiftTestPage page1;
	private OpenShiftTestPage page2;
	private OpenShiftEventTestWizard wizard;
	private WizardDialog wizardBot;

	@Before
	public void setUp() {
		this.wizard = new OpenShiftEventTestWizard();
		Shell shell = new WorkbenchShell().getSWTWidget();
		org.jboss.reddeer.core.util.Display.asyncExec(new Runnable() {

			@Override
			public void run() {
				
				org.eclipse.jface.wizard.WizardDialog dialog = new org.eclipse.jface.wizard.WizardDialog(shell, wizard);
				dialog.create();
				dialog.open();
			}
		});
		new WaitUntil(new ShellWithTextIsActive(OpenShiftEventTestWizard.TITLE));
		wizardBot = new WizardDialog();
	}

	@After
	public void tearDown() {
		DefaultShell activeShell = new DefaultShell();
		if (activeShell.getText().equals(OpenShiftEventTestWizard.TITLE)){
			activeShell.close();
		}
	}
	
	@Test
	public void shouldFireWillGetDeactivatedAndWillGetActivatedWhenSwitchingToNextPage() {
		// given
		// when
		wizardBot.next();
		// then
		OpenShiftTestPage.PageChangeEvent page1Event = page1.willGetDeactivated;
		assertThat(page1Event, is(notNullValue()));
		assertThat(page1Event.getDirection(), is(equalTo(Direction.FORWARDS)));
		OpenShiftTestPage.PageChangeEvent page2Event = page2.willGetActivated;
		assertThat(page2Event, is(notNullValue()));
		assertThat(page2Event.getDirection(), is(equalTo(Direction.FORWARDS)));
	}

	@Test
	public void shouldFireDeactivatedAndActivatedWhenSwitchingToNextPage() {
		// given
		// when
		wizardBot.next();
		// then
		OpenShiftTestPage.PageChangeEvent page1Event = page1.deactivated;
		assertThat(page1Event, is(notNullValue()));
		assertThat(page1Event.getDirection(), is(nullValue()));
		OpenShiftTestPage.PageChangeEvent page2Event = page2.activated;
		assertThat(page2Event, is(notNullValue()));
		assertThat(page2Event.getDirection(), is(nullValue()));
	}

	@Test
	public void shouldFireWillGetActivatedAndWillGetDeactivatedWhenSwitchingToPreviousPage() {
		// given
		// when
		wizardBot.next();
		page1.resetEvents();
		page2.resetEvents();
		wizardBot.back();
		// then
		OpenShiftTestPage.PageChangeEvent page1Event = page1.willGetActivated;
		assertThat(page1Event, is(notNullValue()));
		assertThat(page1Event.getDirection(), is(equalTo(Direction.BACKWARDS)));
		OpenShiftTestPage.PageChangeEvent page2Event = page2.willGetDeactivated;
		assertThat(page2Event, is(notNullValue()));
		assertThat(page2Event.getDirection(), is(equalTo(Direction.BACKWARDS)));
	}

	@Test
	public void shouldFireActivatedAndDeactivatedWhenSwitchingToPreviousPage() {
		// given
		wizardBot.next();
		page1.resetEvents();
		page2.resetEvents();
		// when
		wizardBot.back();
		// then
		OpenShiftTestPage.PageChangeEvent page1Event = page1.activated;
		assertThat(page1Event, is(notNullValue()));
		assertThat(page1Event.getDirection(), is(nullValue()));
		OpenShiftTestPage.PageChangeEvent page2Event = page2.deactivated;
		assertThat(page2Event, is(notNullValue()));
		assertThat(page2Event.getDirection(), is(nullValue()));
	}
	
	private class OpenShiftEventTestWizard extends Wizard {

		private static final String TITLE = "OpenShiftEventTestWizard";
		
		public OpenShiftEventTestWizard() {
			super();
			setWindowTitle(TITLE);
		}

		@Override
		public void addPages() {
			addPage(page1 = new OpenShiftTestPage("page1", wizard));
			addPage(page2 = new OpenShiftTestPage("page2", wizard));
		}

		@Override
		public boolean performFinish() {
			return true;
		}
	}
		
	private class OpenShiftTestPage extends AbstractOpenShiftWizardPage {
		
		private PageChangeEvent activated;
		private PageChangeEvent deactivated;
		private PageChangeEvent willGetActivated;
		private PageChangeEvent willGetDeactivated;

		protected OpenShiftTestPage(String pageName, IWizard wizard) {
			super("title", "description", pageName, wizard);
		}

		@Override
		protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		}

		@Override
		protected void onPageActivated(DataBindingContext dbc) {
			this.activated = new PageChangeEvent();
		}

		@Override
		protected void onPageDeactivated(DataBindingContext dbc) {
			this.deactivated = new PageChangeEvent();
		}

		@Override
		protected void onPageWillGetActivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
			this.willGetActivated = new PageChangeEvent(direction);
		}

		@Override
		protected void onPageWillGetDeactivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
			this.willGetDeactivated = new PageChangeEvent(direction);
		}

		public void resetEvents() {
			this.willGetActivated = null;
			this.activated = null;
			this.willGetDeactivated = null;
			this.deactivated = null;
		}
		
		public class PageChangeEvent {

			private Direction direction;
			private Date timestamp;

			public PageChangeEvent() {
				this(null);
			}

			public PageChangeEvent(Direction direction) {
				this.direction = direction;
				this.timestamp = new Date(); 
			}

			public Direction getDirection() {
				return direction;
			}
		}
	}
}
