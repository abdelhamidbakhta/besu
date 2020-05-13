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
package org.hyperledger.besu.ethereum.eth.transactions;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hyperledger.besu.config.experimental.ExperimentalEIPs;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.eth.transactions.PendingTransactions.TransactionInfo;

import java.util.Collection;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TransactionReplacementByPriceRuleTest {

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return asList(
        new Object[][] {
          {frontierTx(5L), frontierTx(6L), empty(), true},
          {frontierTx(5L), frontierTx(5L), empty(), false},
          {frontierTx(5L), frontierTx(4L), empty(), false},
          {frontierTx(5L), eip1559Tx(3L, 6L), Optional.of(1L), false},
          {frontierTx(5L), eip1559Tx(3L, 5L), Optional.of(3L), false},
          {frontierTx(5L), eip1559Tx(3L, 6L), Optional.of(3L), true},
          {eip1559Tx(3L, 6L), eip1559Tx(3L, 6L), Optional.of(3L), false},
          {eip1559Tx(3L, 6L), eip1559Tx(3L, 7L), Optional.of(3L), false},
          {eip1559Tx(3L, 6L), eip1559Tx(3L, 7L), Optional.of(4L), true},
          {eip1559Tx(3L, 8L), frontierTx(7L), Optional.of(4L), false},
          {eip1559Tx(3L, 8L), frontierTx(8L), Optional.of(4L), true}
        });
  }

  private final TransactionInfo oldTx;
  private final TransactionInfo newTx;
  private final Optional<Long> baseFee;
  private final boolean expected;

  public TransactionReplacementByPriceRuleTest(
      final TransactionInfo oldTx,
      final TransactionInfo newTx,
      final Optional<Long> baseFee,
      final boolean expected) {
    this.oldTx = oldTx;
    this.newTx = newTx;
    this.baseFee = baseFee;
    this.expected = expected;
  }

  @Before
  public void enableEIP1559() {
    ExperimentalEIPs.eip1559Enabled = true;
  }

  @Before
  public void resetEIP1559() {
    ExperimentalEIPs.eip1559Enabled = ExperimentalEIPs.EIP1559_ENABLED_DEFAULT_VALUE;
  }

  @Test
  public void shouldReplace() {
    assertThat(new TransactionReplacementByPriceRule().shouldReplace(oldTx, newTx, baseFee))
        .isEqualTo(expected);
  }

  private static TransactionInfo frontierTx(final long price) {
    final TransactionInfo transactionInfo = mock(TransactionInfo.class);
    final Transaction transaction = mock(Transaction.class);
    when(transaction.getGasPrice()).thenReturn(Wei.of(price));
    when(transactionInfo.getTransaction()).thenReturn(transaction);
    return transactionInfo;
  }

  private static TransactionInfo eip1559Tx(final long gasPremium, final long feeCap) {
    final TransactionInfo transactionInfo = mock(TransactionInfo.class);
    final Transaction transaction = mock(Transaction.class);
    when(transaction.getGasPremium()).thenReturn(Optional.of(Wei.of(gasPremium)));
    when(transaction.getFeeCap()).thenReturn(Optional.of(Wei.of(feeCap)));
    when(transaction.isEIP1559Transaction()).thenReturn(true);
    when(transactionInfo.getTransaction()).thenReturn(transaction);
    return transactionInfo;
  }
}
