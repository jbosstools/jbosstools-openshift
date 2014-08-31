/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test.ui.wizard.snapshot;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.runner.RunWith;
import org.jboss.tools.openshift.express.internal.core.util.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.snapshot.SaveSnapshotWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.snapshot.SaveSnapshotWizardPageModel;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.openshift.client.IApplication;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtils.class)
public class SaveSnapshotWizardPageModelTest {

    private static final String PATH = "/home/username/workspace/app-full.tar.gz";
    private SaveSnapshotWizardModel model;
    private SaveSnapshotWizardPageModel pageModel;
    private IApplication app = mock(IApplication.class);

    @Before
    public void setUp(){
        model = new SaveSnapshotWizardModel(app);
        pageModel = new SaveSnapshotWizardPageModel(model);
    }

    /**
     * This is testing the case for JBIDE-17919 where the destination does not
     * initially flop when a snapshot already exists
     */
    @Test
    public void  destinationShouldBeInitializedWhenPreviousSnapshot(){
        mockStatic(FileUtils.class);
        when(FileUtils.getAvailableFilepath(Mockito.anyString())).thenReturn(PATH);

        model = new SaveSnapshotWizardModel(app);
        pageModel = new SaveSnapshotWizardPageModel(model);

        assertEquals("/home/username/workspace/", pageModel.getDestination());
    }

}
