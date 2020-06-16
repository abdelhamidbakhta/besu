/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.graphql;

import org.hyperledger.besu.ethereum.api.handlers.IsAliveHandler;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.blockcreation.MiningCoordinator;
import org.hyperledger.besu.ethereum.core.Synchronizer;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPool;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;

public class GraphQLDataFetcherContextDecorator implements GraphQLDataFetcherContext {
  private final GraphQLDataFetcherContext core;
  private final IsAliveHandler isAliveHandler;

  public GraphQLDataFetcherContextDecorator(
      final GraphQLDataFetcherContext core, final IsAliveHandler isAliveHandler) {
    this.core = core;
    this.isAliveHandler = isAliveHandler;
  }

  @Override
  public TransactionPool getTransactionPool() {
    return core.getTransactionPool();
  }

  @Override
  public BlockchainQueries getBlockchainQueries() {
    return core.getBlockchainQueries();
  }

  @Override
  public MiningCoordinator getMiningCoordinator() {
    return core.getMiningCoordinator();
  }

  @Override
  public Synchronizer getSynchronizer() {
    return core.getSynchronizer();
  }

  @Override
  public ProtocolSchedule getProtocolSchedule() {
    return core.getProtocolSchedule();
  }

  @Override
  public IsAliveHandler getIsAliveHandler() {
    return isAliveHandler;
  }
}
