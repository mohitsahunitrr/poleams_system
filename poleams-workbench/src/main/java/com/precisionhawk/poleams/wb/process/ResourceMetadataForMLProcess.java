package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.Site;
import com.precisionhawk.ams.support.jackson.ObjectMapperFactory;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ResourceMetadataForMLProcess extends ServiceClientCommandProcess {

    private enum Module {
        Dist,
        Trans
    }

    private static final String ARG_MODULE = "-m";
    private static final String COMMAND = "mlResources";
    private static final String HELP = COMMAND + " " + ARGS_FOR_HELP + "[" + ARG_MODULE + " " + Module.Dist.name() + "|" + Module.Trans.name() + "]";

    private String fileName;
    private Module module;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_MODULE:
                String s = args.poll();
                if (s != null) {
                    module = Module.valueOf(s);
                }
                return module != null;
            default:
                if (fileName == null) {
                    fileName = arg;
                    return true;
                }
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        WSClientHelper svcs = new WSClientHelper(env);

        List<ResourceMetadata> resources = new LinkedList<>();

        OutputStream out = null;
        try {
            if (module == null || module == Module.Dist) {
                for (Feeder f : svcs.feeders().retrieveAll(svcs.token())) {
                    addResources(svcs, resources, f);
                }
            }
            if (module == null || module == Module.Trans) {
                for (TransmissionLine l : svcs.transmissionLines().retrieveAll(svcs.token())) {
                    addResources(svcs, resources, l);
                }
            }
            System.out.printf("Writing data to %s\n", fileName);
            out = new FileOutputStream(fileName);
            ObjectMapperFactory.getObjectMapper().writeValue(out, resources);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        } finally {
            IOUtils.closeQuietly(out);
        }

        return true;
    }

    private void addResources(WSClientHelper svcs, List<ResourceMetadata> resources, Site site) throws IOException {
        System.out.printf("Gathering images for %s\n", site.getName());
        ResourceSearchParams params = new ResourceSearchParams();
        params.setSiteId(site.getId());
        params.setType(ResourceTypes.DroneInspectionImage);
        resources.addAll(svcs.resources().search(svcs.token(), params));
        params.setType(ResourceTypes.Thermal);
        resources.addAll(svcs.resources().search(svcs.token(), params));
    }

    @Override
    public boolean canProcess(String s) {
        return COMMAND.equals(s);
    }

    @Override
    public void printHelp(PrintStream printStream) {
        printStream.println(HELP);
    }
}
