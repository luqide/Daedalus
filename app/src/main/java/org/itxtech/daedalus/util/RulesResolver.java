package org.itxtech.daedalus.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class RulesResolver implements Runnable {
    private static final String TAG = "DRulesResolver";

    public static final int STATUS_LOADED = 0;
    public static final int STATUS_LOADING = 1;
    public static final int STATUS_NOT_LOADED = 2;
    public static final int STATUS_PENDING_LOAD = 3;

    public static final int MODE_HOSTS = 0;
    public static final int MODE_DNSMASQ = 1;

    private static int status = STATUS_NOT_LOADED;
    private static int mode = MODE_HOSTS;
    private static String hostsFile;
    private static String dnsmasqFile;
    private static HashMap<String, String> rules;
    private static boolean shutdown = false;

    public RulesResolver() {
        status = STATUS_NOT_LOADED;
        hostsFile = "";
        dnsmasqFile = "";
        shutdown = false;
    }

    public static void shutdown() {
        shutdown = true;
    }

    public static boolean isLoaded() {
        return status == STATUS_LOADED;
    }

    public static void startLoadHosts(String loadFile) {
        Log.d(TAG, "Loading hosts file " + loadFile);
        hostsFile = loadFile;
        mode = MODE_HOSTS;
        status = STATUS_PENDING_LOAD;
    }

    public static void startLoadDnsmasq(String loadFile) {
        Log.d(TAG, "Loading DNSMasq file " + loadFile);
        dnsmasqFile = loadFile;
        mode = MODE_DNSMASQ;
        status = STATUS_PENDING_LOAD;
    }

    public static void clean() {
        rules = null;
    }

    public static String resolve(String hostname) {
        if (rules == null) {
            return null;
        }
        if (rules.containsKey(hostname)) {
            return rules.get(hostname);
        }
        if (mode == MODE_DNSMASQ) {
            String[] pieces = hostname.split("\\.");
            StringBuilder builder;
            for (int i = 1; i < pieces.length; i++) {
                builder = new StringBuilder();
                for (int j = i; j < pieces.length; j++) {
                    builder.append(pieces[j]);
                    if (j < pieces.length - 1) {
                        builder.append(".");
                    }
                }
                if (rules.containsKey(builder.toString())) {
                    return rules.get(builder.toString());
                }
            }
        }
        return null;
    }

    private void load() {
        try {
            status = STATUS_LOADING;
            rules = new HashMap<>();
            if (mode == MODE_HOSTS) {
                File file = new File(hostsFile);
                if (file.exists() && file.canRead()) {
                    FileInputStream stream = new FileInputStream(file);
                    BufferedReader dataIO = new BufferedReader(new InputStreamReader(stream));
                    String strLine;
                    String[] data;
                    while ((strLine = dataIO.readLine()) != null) {
                        //Log.d(TAG, strLine);
                        if (!strLine.equals("") && !strLine.startsWith("#")) {
                            data = strLine.split("\\s+");
                            rules.put(data[1], data[0]);
                            Log.d(TAG, "Putting " + data[0] + " " + data[1]);
                        }
                    }

                    dataIO.close();
                    stream.close();
                }
            } else if (mode == MODE_DNSMASQ) {
                File file = new File(dnsmasqFile);
                if (file.exists() && file.canRead()) {
                    FileInputStream stream = new FileInputStream(file);
                    BufferedReader dataIO = new BufferedReader(new InputStreamReader(stream));
                    String strLine;
                    String[] data;
                    while ((strLine = dataIO.readLine()) != null) {
                        //Log.d(TAG, strLine);
                        if (!strLine.equals("") && !strLine.startsWith("#")) {
                            data = strLine.split("/");
                            if (data.length == 3 && data[0].equals("address=")) {
                                if (data[1].startsWith(".")) {
                                    data[1] = data[1].substring(1, data[1].length());
                                }
                                rules.put(data[1], data[2]);
                                Log.d(TAG, "Putting " + data[1] + " " + data[2]);
                            }
                        }
                    }

                    dataIO.close();
                    stream.close();
                }
            }
            status = STATUS_LOADED;
        } catch (Exception e) {
            Log.e(TAG, e.toString());

            status = STATUS_NOT_LOADED;
        }
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                if (status == STATUS_PENDING_LOAD) {
                    load();
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
