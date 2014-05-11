/*
 * This code contains copyright information which is the proprietary property
 * of SITA Advanced Travel Solutions. No part of this code may be reproduced,
 * stored or transmitted in any form without the prior written permission of
 * SITA Advanced Travel Solutions.
 *
 * Copyright SITA Advanced Travel Solutions 2013
 * All rights reserved.
 */
package com.sai.scripts;

import java.io.InputStream;

import com.jcraft.jsch.*;

/**
 * 
 * @author Sai Kris
 * 
 */
public final class SshUtil {

    private SshUtil() {
    }

    public static String run(final String sshUser, final String sshPassword, final String host, final String command) {
        JSch jsch = new JSch();
        Session session = null;
        Channel channel = null;

        StringBuilder out = new StringBuilder();
        try {

            session = jsch.getSession(sshUser, host, 22);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000); // making a connection with timeout.
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            channel.connect();

            InputStream in = channel.getInputStream();
            byte[] bt = new byte[1024];

            while (true) {

                while (in.available() > 0) {
                    int i = in.read(bt, 0, 1024);
                    if (i < 0)
                        break;
                    String str = new String(bt, 0, i);
                    // displays the output of the command executed.
                    out.append(str);

                }
                if (channel.isClosed()) {

                    break;
                }
                Thread.sleep(1000);

            }
        } catch (Exception e) {
            out.append(e.toString());
            throw new RuntimeException(e);
        } finally {
            channel.disconnect();
            session.disconnect();
        }
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        String output = run("config", "config", "10.70.101.17", "yum deplist sitaOmanUserPortal");
        System.out.println(output);
    }
}
