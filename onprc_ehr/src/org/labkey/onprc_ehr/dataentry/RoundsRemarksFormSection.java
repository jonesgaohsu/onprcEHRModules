/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.onprc_ehr.dataentry;

import org.labkey.api.ehr.EHRService;
import org.labkey.api.ehr.dataentry.SimpleFormSection;
import org.labkey.api.view.template.ClientDependency;

import java.util.List;

/**
 * User: bimber
 * Date: 4/27/13
 * Time: 10:54 AM
 */
public class RoundsRemarksFormSection extends SimpleFormSection
{
    public RoundsRemarksFormSection(String label, EHRService.FORM_SECTION_LOCATION location)
    {
        super("study", "Clinical Remarks", label, "ehr-roundsremarksgridpanel", location);
        addClientDependency(ClientDependency.fromFilePath("ehr/plugin/ClinicalObservationsCellEditing.js"));
        addClientDependency(ClientDependency.fromFilePath("ehr/panel/ClinicalRemarkPanel.js"));
        addClientDependency(ClientDependency.fromFilePath("ehr/grid/RoundsRemarksGridPanel.js"));
        addClientDependency(ClientDependency.fromFilePath("ehr/grid/ObservationsRowEditorGridPanel.js"));
        addClientDependency(ClientDependency.fromFilePath("ehr/plugin/ClinicalRemarksRowEditor.js"));
        addClientDependency(ClientDependency.fromFilePath("ehr/data/ClinicalObservationsClientStore.js"));
        addClientDependency(ClientDependency.fromFilePath("ehr/buttons/roundsButtons.js"));

        setTemplateMode(TEMPLATE_MODE.NONE);
    }

    @Override
    public List<String> getTbarButtons()
    {
        List<String> defaultButtons = super.getTbarButtons();
        defaultButtons.remove("COPYFROMSECTION");
        defaultButtons.remove("ADDRECORD");
        defaultButtons.remove("ADDANIMALS");

        if (defaultButtons.contains("DELETERECORD"))
        {
            int idx = defaultButtons.indexOf("DELETERECORD");
            defaultButtons.remove("DELETERECORD");
            defaultButtons.add(idx, "ROUNDSDELETE");
        }

        defaultButtons.add("MARK_ROUNDS_REVIEWED");

        return defaultButtons;
    }

    @Override
    public List<String> getTbarMoreActionButtons()
    {
        List<String> defaultButtons = super.getTbarMoreActionButtons();
        defaultButtons.remove("DUPLICATE");

        return defaultButtons;
    }
}
