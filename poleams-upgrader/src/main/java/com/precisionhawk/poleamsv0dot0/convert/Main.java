package com.precisionhawk.poleamsv0dot0.convert;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class Main {
    
    private static final String ARG_SOURCE_CLUSTER = "-sc";
    private static final String ARG_SOURCE_HOST = "-sh";
    private static final String ARG_TARGET_CLUSTER = "-tc";
    private static final String ARG_TARGET_HOST = "-th";

    public static void main(String[] argsArray) {
        ElasticSearchConfigBean sourceESConfig = new ElasticSearchConfigBean();
        ElasticSearchConfigBean targetESConfig = new ElasticSearchConfigBean();
        
        Queue<String> args = new LinkedList<>();
        Collections.addAll(args, argsArray);
        while (!args.isEmpty()) {
            String arg = args.poll();
            switch (arg) {
                case ARG_SOURCE_CLUSTER:
                    if (sourceESConfig.getClusterName() == null) {
                        sourceESConfig.setClusterName(args.poll());
                    } else {
                        System.err.println("Source index (" + ARG_SOURCE_CLUSTER + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                case ARG_SOURCE_HOST:
                    if (sourceESConfig.getNodeHosts() == null) {
                        sourceESConfig.setNodeHosts(args.poll());
                    } else {
                        System.err.println("Source hosts (" + ARG_SOURCE_HOST + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                case ARG_TARGET_CLUSTER:
                    if (targetESConfig.getClusterName() == null) {
                        targetESConfig.setClusterName(args.poll());
                    } else {
                        System.err.println("Target index (" + ARG_TARGET_CLUSTER + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                case ARG_TARGET_HOST:
                    if (targetESConfig.getNodeHosts() == null) {
                        targetESConfig.setNodeHosts(args.poll());
                    } else {
                        System.err.println("Target hosts (" + ARG_TARGET_HOST + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                default:
                    System.err.printf("Invalid argument \"%s\".\n", arg);
                    System.exit(1);
            }
        }
        if (!sourceESConfig.isValid()) {
            System.err.println("Invalid or missing config for source ES cluster.");
            System.exit(1);
        }
        if (!targetESConfig.isValid()) {
            System.err.println("Invalid or missing config for target ES cluster.");
            System.exit(1);
        }
    }
}
