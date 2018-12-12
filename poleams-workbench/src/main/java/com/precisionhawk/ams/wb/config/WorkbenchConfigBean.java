package com.precisionhawk.ams.wb.config;

import com.precisionhawk.ams.wb.process.CommandProcess;
import com.precisionhawk.poleams.wb.process.ChangeResourceStatusProcess;
import com.precisionhawk.poleams.wb.process.DeletePoleProcess;
import com.precisionhawk.poleams.wb.process.FeederDataImportProcess;
import com.precisionhawk.poleams.wb.process.GeoJsonMasterDataImportProcess;
import com.precisionhawk.poleams.wb.process.PopulateEncroachmentGoogleEarthURL;
import com.precisionhawk.poleams.wb.process.PopulateMasterSurveyProcess;
import com.precisionhawk.poleams.wb.process.ResourceUploadProcess;
import com.precisionhawk.poleams.wb.process.TransmissionLineInspectionImportProcess;
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
        COMMANDS.add(new PopulateEncroachmentGoogleEarthURL());
        COMMANDS.add(new PopulateMasterSurveyProcess());
        COMMANDS.add(new ResourceUploadProcess());
        COMMANDS.add(new TransmissionLineInspectionImportProcess());
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
