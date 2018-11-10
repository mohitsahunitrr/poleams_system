package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author pchapman
 */
public class DeletePoleProcess extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "deletePole";
    private static final String HELP =
            "\t" + COMMAND + " FPL_Id [FPL_Id] [FPL_Id] [...]";

    private Set<String> ids = new HashSet<>();
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        ids.add(arg);
        return true;
    }

    @Override
    protected boolean execute(Environment env) {
        if (ids.isEmpty()) {
            return false;
        }
        for (String fplId : ids) {
            try {
                deletePole(env, fplId);
            } catch (IOException ex) {
                System.err.printf("Error looking up or deleting pole with FPL ID %s\n", fplId);
            }
        }
        return true;
    }
    
    private void deletePole(Environment env, String fplId) throws IOException {
        PoleSearchParams params = new PoleSearchParams();
        params.setFPLId(fplId);
        Pole p = CollectionsUtilities.firstItemIn(poleService(env).search(env.obtainAccessToken(), params));
        if (p == null) {
            System.out.printf("No pole with FPL ID %s exists.  Nothing to delete.\n", fplId);
        } else {
            poleService(env).delete(env.obtainAccessToken(), p.getId());
            System.out.printf("Pole with FPL ID %s deleteed.\n", fplId);
        }
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.println(HELP);
    }
    
}
