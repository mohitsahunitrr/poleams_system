package com.precisionhawk.ams.wb.config;

import com.precisionhawk.ams.wb.process.CommandProcess;
import com.precisionhawk.poleams.wb.process.*;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class WorkbenchConfigBean implements WorkbenchConfig {

    private final List<CommandProcess> COMMANDS = new LinkedList<>();
    
    public WorkbenchConfigBean() {
        COMMANDS.add(new ChangeResourceStatusProcess());
        COMMANDS.add(new CreateOrganizationProcess());
        COMMANDS.add(new DataCopyProcess());
        COMMANDS.add(new DeletePoleProcess());
        COMMANDS.add(new DeleteSiteProcess());
        COMMANDS.add(new DukeDeltaImport());
        COMMANDS.add(new DukeInspectionImport());
        COMMANDS.add(new DukeInventoryImport());
        COMMANDS.add(new ResourceUploadProcess());
        COMMANDS.add(new FeederDataImportProcess());
        COMMANDS.add(new FixDataProcess());
        COMMANDS.add(new DistributionMasterDataImportProcess());
        COMMANDS.add(new InspectShapefile());
        COMMANDS.add(new OrgFieldConfigsUploadProcess());
        COMMANDS.add(new PopulateEncroachmentGoogleEarthURL());
        COMMANDS.add(new PopulateMasterSurveyProcess());
        COMMANDS.add(new PPLInspectionFindingsImportProcess());
        COMMANDS.add(new QuerySiteInfo());
        COMMANDS.add(new ResourceMetadataForMLProcess());
        COMMANDS.add(new ResourceScaleProcess());
        COMMANDS.add(new ResourceUploadProcess());
        COMMANDS.add(new TransmissionLineInspectionImportProcess());
        COMMANDS.add(new TransmissionMasterDataImportProcess());
        COMMANDS.add(new ZoomifyUploadProcess());
    };
    
    @Override
    public String getConfigDirName() {
        return "poleams";
    }

    @Override
    public List<CommandProcess> getCommands() {
        return COMMANDS;
    }
    
}
