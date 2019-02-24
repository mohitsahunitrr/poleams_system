/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.processors.ProcessListener;

/**
 *
 * @author pchapman
 */
public class CLIProcessListener implements ProcessListener {
    @Override
    public void reportFatalError(String message) {
        System.err.println(message);
    }
    @Override
    public void reportFatalException(String message, Throwable t) {
        System.err.println(message);
        t.printStackTrace(System.err);
    }
    @Override
    public void reportFatalException(Exception ex) {
        ex.printStackTrace(System.err);
    }
    @Override
    public void reportMessage(String message) {
        System.out.println(message);
    }
    @Override
    public void reportNonFatalError(String message) {
        System.err.println(message);
    }
    @Override
    public void reportNonFatalException(String message, Throwable t) {
        System.err.println(message);
        t.printStackTrace(System.err);
    }    
}
