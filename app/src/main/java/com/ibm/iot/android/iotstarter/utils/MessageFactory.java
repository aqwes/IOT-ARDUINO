/*******************************************************************************
 * Copyright (c) 2014-2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *    Aldo Eisma - add bearing and speed to acceleration message
 *******************************************************************************/
package com.ibm.iot.android.iotstarter.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Build messages to be published by the application.
 * This class is currently unused.
 */
public class MessageFactory {

    public static String getAccelMessage(String movement) {
        return "{ \"d\": {" +
                "\"movement\":\"" + movement + "\" " +
                "} }";
    }
}
