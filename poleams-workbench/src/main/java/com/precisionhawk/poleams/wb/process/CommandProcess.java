/*
 * All rights reserved.
 */
package com.precisionhawk.poleams.wb.process;

import java.io.PrintStream;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class CommandProcess {
    
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    public abstract boolean canProcess(String command);
    
    public abstract void printHelp(PrintStream output);
    
    public abstract boolean process(Queue<String> args);
}
