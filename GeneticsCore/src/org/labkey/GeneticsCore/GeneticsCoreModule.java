/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.GeneticsCore;

import org.jetbrains.annotations.NotNull;
import org.labkey.GeneticsCore.analysis.MethylationRateComparison;
import org.labkey.GeneticsCore.button.ChangeReadsetStatusButton;
import org.labkey.GeneticsCore.button.PublishSBTResultsButton;
import org.labkey.GeneticsCore.button.SBTReviewButton;
import org.labkey.GeneticsCore.notification.GeneticsCoreNotification;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.laboratory.LaboratoryService;
import org.labkey.api.ldk.ExtendedSimpleModule;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.ldk.table.SimpleButtonConfigFactory;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.sequenceanalysis.SequenceAnalysisService;
import org.labkey.api.view.template.ClientDependency;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class GeneticsCoreModule extends ExtendedSimpleModule
{
    public static final String NAME = "GeneticsCore";
    public static final String CONTROLLER_NAME = "geneticscore";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public double getVersion()
    {
        return 0.02;
    }

    @Override
    public boolean hasScripts()
    {
        return false;
    }

    @Override
    protected void doStartupAfterSpringConfig(ModuleContext moduleContext)
    {
        SimpleButtonConfigFactory btn1 = new SimpleButtonConfigFactory(this, "Add Genetics Blood Draw Flags", "GeneticsCore.buttons.editGeneticsFlagsForSamples(dataRegionName, arguments[0] ? arguments[0].ownerCt : null, 'add')");
        btn1.setClientDependencies(ClientDependency.fromModuleName("laboratory"), ClientDependency.fromModuleName("ehr"), ClientDependency.fromPath("geneticscore/window/ManageFlagsWindow.js"), ClientDependency.fromPath("geneticscore/buttons.js"));
        LDKService.get().registerQueryButton(btn1, "laboratory", "samples");

        SimpleButtonConfigFactory btn2 = new SimpleButtonConfigFactory(this, "Remove Genetics Blood Draw Flags", "GeneticsCore.buttons.editGeneticsFlagsForSamples(dataRegionName, arguments[0] ? arguments[0].ownerCt : null, 'remove')");
        btn2.setClientDependencies(ClientDependency.fromModuleName("laboratory"), ClientDependency.fromModuleName("ehr"), ClientDependency.fromPath("geneticscore/window/ManageFlagsWindow.js"), ClientDependency.fromPath("geneticscore/buttons.js"));
        LDKService.get().registerQueryButton(btn2, "laboratory", "samples");

        NotificationService.get().registerNotification(new GeneticsCoreNotification());

        LDKService.get().registerQueryButton(new SBTReviewButton(), "sequenceanalysis", "sequence_analyses");
        LDKService.get().registerQueryButton(new ChangeReadsetStatusButton(), "sequenceanalysis", "sequence_analyses");
        LDKService.get().registerQueryButton(new PublishSBTResultsButton(), "sequenceanalysis", "alignment_summary_by_lineage");
        LaboratoryService.get().registerTableCustomizer(this, GeneticsTableCustomizer.class, "sequenceanalysis", "sequence_analyses");

        SequenceAnalysisService.get().registerFileHandler(new MethylationRateComparison());
    }

    @Override
    protected void init()
    {
        addController(CONTROLLER_NAME, GeneticsCoreController.class);
    }

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.emptySet();
    }

    @Override
    @NotNull
    public Set<DbSchema> getSchemasToTest()
    {
        return Collections.emptySet();
    }
}
