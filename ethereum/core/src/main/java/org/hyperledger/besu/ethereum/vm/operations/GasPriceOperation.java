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
package org.hyperledger.besu.ethereum.vm.operations;

import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.vm.EVM;
import org.hyperledger.besu.ethereum.vm.GasCalculator;
import org.hyperledger.besu.ethereum.vm.MessageFrame;

public class GasPriceOperation extends AbstractFixedCostOperation {

  public GasPriceOperation(final GasCalculator gasCalculator) {
    super(0x3A, "GASPRICE", 0, 1, false, 1, gasCalculator, gasCalculator.getBaseTierGasCost());
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    if (frame.getRemainingGas().compareTo(gasCost) < 0) {
      return outOfGasResponse;
    }

    final Wei gasPrice = frame.getGasPrice();
    frame.pushStackItem(gasPrice.toBytes());

    return successResponse;
  }
}
