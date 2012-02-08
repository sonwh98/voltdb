/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltcore.zk;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import org.apache.zookeeper_voltpatches.WatchedEvent;
import org.apache.zookeeper_voltpatches.Watcher;
import org.apache.zookeeper_voltpatches.ZooKeeper;
import org.apache.zookeeper_voltpatches.Watcher.Event.KeeperState;
import org.voltdb.VoltDB;
import org.voltcore.agreement.AgreementSite;
import org.voltdb.fault.FaultHandler;
import org.voltdb.fault.VoltFault;
import org.voltdb.fault.VoltFault.FaultType;
import org.voltcore.messaging.HostMessenger;
import org.voltcore.messaging.Mailbox;
import org.voltcore.messaging.MessagingException;
import org.voltcore.messaging.Subject;
import org.voltcore.messaging.VoltMessage;
import org.voltcore.utils.DBBPool;

/**
 *
 */
public class ZKTestBase {
    protected ArrayList<HostMessenger> m_messengers;
    protected ArrayList<ZooKeeper> m_clients;

    protected void setUpZK(int sites) throws Exception {
        m_clients = new ArrayList<ZooKeeper>();
        m_messengers = new ArrayList<HostMessenger>();
        for (int ii = 0; ii < sites; ii++) {
            HostMessenger.Config config = new HostMessenger.Config();
            config.internalPort += ii;
            config.zkInterface = "127.0.0.1:" + (2182 + ii);
            config.networkThreads = 1;
            HostMessenger hm = new HostMessenger(config);
            hm.start();
            m_messengers.add(hm);
        }
    }

    protected void tearDownZK() throws Exception {
        for (ZooKeeper keeper : m_clients) {
            System.out.println("Shutting down client " + keeper);
            keeper.close();
        }
        m_clients.clear();
        for (HostMessenger hm : m_messengers) {
            System.out.println("Shutting down messenger " + hm);
            if (hm != null) {
                hm.shutdown();
            }
        }
        m_messengers.clear();
    }

    protected ZooKeeper getClient(int site) throws Exception {
        final Semaphore permit = new Semaphore(0);
        ZooKeeper keeper = new ZooKeeper("localhost:" + Integer.toString(2182 + site), 4000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == KeeperState.SyncConnected) {
                    permit.release();
                }
                System.out.println(event);
            }});
        m_clients.add(keeper);
        permit.acquire();
        return keeper;
    }
}