/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.protocol.protobuf.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.tools.javac.util.List;
import org.apache.geode.cache.client.internal.locator.GetAllServersResponse;
import org.apache.geode.distributed.internal.DM;
import org.apache.geode.distributed.internal.ServerLocation;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.distributed.internal.membership.MembershipManager;
import org.apache.geode.distributed.internal.membership.NetView;
import org.apache.geode.distributed.internal.tcpserver.TcpClient;
import org.apache.geode.internal.cache.CacheServerImpl;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.ServerAPI;
import org.apache.geode.protocol.protobuf.ServerAPI.GetAvailableServersResponse;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.protocol.protobuf.utilities.ProtobufRequestUtilities;
import org.apache.geode.test.junit.categories.UnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

@Category(UnitTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest(GetAvailableServersOperationHandler.class)
public class GetAvailableServersOperationHandlerJUnitTest extends OperationHandlerJUnitTest {

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testServerReturnedFromHandler() throws Exception {
    operationHandler = mock(GetAvailableServersOperationHandler.class);
    cacheStub = mock(GemFireCacheImpl.class);
    when(operationHandler.process(any(), any(), any())).thenCallRealMethod();

    CacheServerImpl mockCacheServer = mock(CacheServerImpl.class);
    DM mockDM = mock(DM.class);
    MembershipManager mockMembershipManager = mock(MembershipManager.class);
    NetView mockView = mock(NetView.class);
    InternalDistributedMember mockCoordinator = mock(InternalDistributedMember.class);
    TcpClient mockTCPClient = PowerMockito.mock(TcpClient.class, (InvocationOnMock invocation) -> new GetAllServersResponse(new ArrayList(List.of(new ServerLocation("hostname1", 12345),
        new ServerLocation("hostname2", 23456)))));

    when(cacheStub.getCacheServers()).thenReturn(List.of(mockCacheServer));
    when(mockCacheServer.getExternalAddress()).thenReturn("externalHostName");
    when(mockCacheServer.getPort()).thenReturn(23456);
    when(((GemFireCacheImpl) cacheStub).getDistributionManager()).thenReturn(mockDM);
    when(mockDM.getMembershipManager()).thenReturn(mockMembershipManager);
    when(mockMembershipManager.getView()).thenReturn(mockView);
    when(mockView.getCoordinator()).thenReturn(mockCoordinator);

    when(((GetAvailableServersOperationHandler) operationHandler).getTcpClient())
        .thenReturn(mockTCPClient);

    ServerAPI.GetAvailableServersRequest
        getAvailableServersRequest =
        ProtobufRequestUtilities.createGetAvailableServersRequest();
    Result
        operationHandlerResult =
        operationHandler.process(serializationServiceStub, getAvailableServersRequest, cacheStub);
    assertTrue(operationHandlerResult instanceof Success);
    GetAvailableServersResponse
        getAvailableServersResponse =
        (GetAvailableServersResponse) operationHandlerResult.getMessage();

    assertEquals(2, getAvailableServersResponse.getServersCount());
    BasicTypes.Server server = getAvailableServersResponse.getServers(0);
    assertEquals("hostname1", server.getHostname());
    assertEquals(12345, server.getPort());
    server = getAvailableServersResponse.getServers(1);
    assertEquals("hostname2", server.getHostname());
    assertEquals(23456, server.getPort());
  }
}