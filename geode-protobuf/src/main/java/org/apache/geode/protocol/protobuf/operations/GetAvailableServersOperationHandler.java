/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.client.internal.locator.GetAllServersRequest;
import org.apache.geode.cache.client.internal.locator.GetAllServersResponse;
import org.apache.geode.distributed.internal.ServerLocation;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.distributed.internal.tcpserver.TcpClient;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.ServerAPI;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.serialization.SerializationService;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class GetAvailableServersOperationHandler implements OperationHandler<ServerAPI.GetAvailableServersRequest,ServerAPI.GetAvailableServersResponse> {

  @Override
  public Result<ServerAPI.GetAvailableServersResponse> process(
      SerializationService serializationService, ServerAPI.GetAvailableServersRequest request,
      Cache cache) {

    //TODO Iterate over "known" locators, connect to first one and execute "GetAllServersRequest" message
    /*
    InternalDistributedSystem distributedSystem =
        (InternalDistributedSystem) cache.getDistributedSystem();
    Properties properties = distributedSystem.getProperties();
    String locatorsString = properties.getProperty(ConfigurationProperties.LOCATORS);*/

    InternalDistributedMember
        coordinator =
        ((GemFireCacheImpl) cache).getDistributionManager().getMembershipManager().getView()
            .getCoordinator();

    TcpClient tcpClient = getTcpClient();
    try {
      GetAllServersResponse getAllServersResponse =
          (GetAllServersResponse) tcpClient.requestToServer(coordinator.getInetAddress(), coordinator.getPort(),
              new GetAllServersRequest(), 1000, true);
      Collection<BasicTypes.Server> servers =
          (Collection<BasicTypes.Server>) getAllServersResponse.getServers().stream()
              .map(serverLocation -> getServerProtobufMessage(
                  (ServerLocation) serverLocation)).collect(Collectors.toList());
      ServerAPI.GetAvailableServersResponse.Builder builder =
          ServerAPI.GetAvailableServersResponse.newBuilder().addAllServers(servers);
      return Success.of(builder.build());
    } catch (IOException | ClassNotFoundException e) {
        return Failure.of(BasicTypes.ErrorResponse.newBuilder().setMessage(e.getMessage()).build());
    }
  }

  protected TcpClient getTcpClient() {
    return new TcpClient();
  }

  private BasicTypes.Server getServerProtobufMessage(ServerLocation serverLocation) {
    BasicTypes.Server.Builder serverBuilder = BasicTypes.Server.newBuilder();
    serverBuilder.setHostname(serverLocation.getHostName()).setPort(serverLocation.getPort());
    return serverBuilder.build();
  }
}
