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
        COMMANDS.add(new DeletePoleProcess());
        COMMANDS.add(new FeederDataImportProcess());
        COMMANDS.add(new GeoJsonMasterDataImportProcess());
        COMMANDS.add(new OrgFieldConfigsUploadProcess());
        COMMANDS.add(new PopulateEncroachmentGoogleEarthURL());
        COMMANDS.add(new PopulateMasterSurveyProcess());
        COMMANDS.add(new ResourceScaleProcess());
        COMMANDS.add(new ResourceUploadProcess());
        COMMANDS.add(new TransmissionLineInspectionImportProcess());
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
