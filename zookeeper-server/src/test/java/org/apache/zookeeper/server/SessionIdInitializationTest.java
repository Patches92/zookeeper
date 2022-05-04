/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server;

import java.util.Map;
import java.util.HashMap;


import org.junit.Assert;
import org.junit.Test;
import org.junit.BeforeClass;

/**
 * Testing zk client session initialization logic in SessionTrackerImpl
 * with and without the fix provided in ZOOKEEPER-1622
 */
public class SessionIdInitializationTest {

    private static final long _2022_05 = 1651559872847L;

    private static final long _2021_05 = 1651559872847L - 365L*24*60*60*1000;


    /**
     * Generates an initial sessionId. High order byte is serverId, next 5
     * 5 bytes are from timestamp, and low order 2 bytes are 0s.
     */
    public static long initializeNextSession(long id, boolean withZooKeeper1622, long currentTimeMillis) {
        long nextSid = 0;
        if(withZooKeeper1622) {
            nextSid = (currentTimeMillis << 24) >>> 8;
        } else {
            nextSid = (currentTimeMillis << 24) >> 8;
        }
        nextSid =  nextSid | (id <<56);
        return nextSid;
    }


    @BeforeClass
    public static void setup() {
        System.out.println("==============================================");
        System.out.println("2022 05 millis: " + _2022_05);
        System.out.println(long2binary(_2022_05));
        System.out.println("2021 05 millis: " + _2021_05);
        System.out.println(long2binary(_2021_05));
        System.out.println("==============================================");
    }


    @Test
    public void test2021WithZooKeeper1622() {
        System.out.println("\n =============== 2021-05 , with ZOOKEEPER-1622 =====================");
        test(true, _2021_05);
    }

    @Test
    public void test2022WithZooKeeper1622() {
        System.out.println("\n =============== 2022-05 , with ZOOKEEPER-1622 =====================");
        test(true, _2022_05);
    }


    @Test
    public void test2021WithoutZooKeeper1622() {
        System.out.println("\n =============== 2021-05 , without ZOOKEEPER-1622 =====================");
        test(false, _2021_05);
    }

    @Test
    public void test2022WithoutZooKeeper1622() {
        System.out.println("\n =============== 2022-05 , without ZOOKEEPER-1622 =====================");
        test(false, _2022_05);
    }

    public void test(boolean withZooKeeper1622, long currentTimeMillisec) {
        Map<String, Long> initialSessionIds = new HashMap<>();
        boolean failed = false;
        for(long myid = 1; myid < 256; myid++) {
            String initialSessionId = long2binary(initializeNextSession(myid, withZooKeeper1622, currentTimeMillisec));
            System.out.println("myid: " + String.format("%3d", myid) + " initialSessionId: " + initialSessionId);
            Long duplicatedId = initialSessionIds.get(initialSessionId);
            if(duplicatedId != null) {
                failed = true;
                System.out.println(String.format("initialSessionId collision! same session ids: server-1 = %d, server-2 = %d", myid, duplicatedId));
            } else {
                initialSessionIds.put(initialSessionId, myid);
            }
        }
        Assert.assertFalse(failed);
    }

    private static String long2binary(long l) {
        String str = String.format("%64s", Long.toBinaryString(l)).replace(' ', '0');
        String splitted = "";
        for(int i=0; i<8; i++) {
            splitted += " ";
            splitted += str.substring(i*8, (i+1)*8);
        }
        return splitted;
    }
}
