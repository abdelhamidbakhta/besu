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
package org.hyperledger.besu.ethereum.blockcreation;

import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.chain.MinedBlockObserver;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockImporter;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.mainnet.HeaderValidationMode;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.plugin.data.Quantity;
import org.hyperledger.besu.util.Subscribers;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for creating a block, and importing it to the blockchain. This is specifically a
 * mainnet capability (as IBFT would then use the block as part of a proposal round).
 *
 * <p>While the capability is largely functional, it has been wrapped in an object to allow it to be
 * cancelled safely.
 *
 * <p>This class is responsible for mining a single block only - the AbstractBlockCreator maintains
 * state so must be destroyed between block mining activities.
 */
public class BlockMiner<C, M extends AbstractBlockCreator<C>> implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  protected final Function<BlockHeader, M> blockCreatorFactory;
  protected final M minerBlockCreator;

  protected final ProtocolContext<C> protocolContext;
  protected final BlockHeader parentHeader;

  private final ProtocolSchedule<C> protocolSchedule;
  private final Subscribers<MinedBlockObserver> observers;
  private final AbstractBlockScheduler scheduler;
  // private static Long lastBaseFee = null;

  public BlockMiner(
      final Function<BlockHeader, M> blockCreatorFactory,
      final ProtocolSchedule<C> protocolSchedule,
      final ProtocolContext<C> protocolContext,
      final Subscribers<MinedBlockObserver> observers,
      final AbstractBlockScheduler scheduler,
      final BlockHeader parentHeader) {
    this.blockCreatorFactory = blockCreatorFactory;
    this.minerBlockCreator = blockCreatorFactory.apply(parentHeader);
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.observers = observers;
    this.scheduler = scheduler;
    this.parentHeader = parentHeader;
  }

  @Override
  public void run() {

    boolean blockMined = false;
    while (!blockMined && !minerBlockCreator.isCancelled()) {
      try {
        blockMined = mineBlock();
      } catch (final CancellationException ex) {
        LOG.debug("Block creation process cancelled.");
        break;
      } catch (final InterruptedException ex) {
        LOG.debug("Block mining was interrupted.", ex);
        Thread.currentThread().interrupt();
      } catch (final Exception ex) {
        LOG.error("Block mining threw an unhandled exception.", ex);
      }
    }
  }

  /**
   * Create a block with the given transactions and ommers. The list of transactions are validated
   * as they are processed, and are not guaranteed to be included in the final block. If
   * transactions must match exactly, the caller must verify they were all able to be included.
   *
   * @param parentHeader The header of the parent of the block to be produced
   * @param transactions The list of transactions which may be included.
   * @param ommers The list of ommers to include.
   * @return the newly created block.
   */
  public Block createBlock(
      final BlockHeader parentHeader,
      final List<Transaction> transactions,
      final List<BlockHeader> ommers) {
    final BlockCreator blockCreator = this.blockCreatorFactory.apply(parentHeader);
    final long timestamp = scheduler.getNextTimestamp(parentHeader).getTimestampForHeader();
    return blockCreator.createBlock(transactions, ommers, timestamp);
  }

  protected boolean mineBlock() throws InterruptedException {
    // Ensure the block is allowed to be mined - i.e. the timestamp on the new block is sufficiently
    // ahead of the parent, and still within allowable clock tolerance.
    LOG.trace("Started a mining operation.");

    final long newBlockTimestamp = scheduler.waitUntilNextBlockCanBeMined(parentHeader);

    final Stopwatch stopwatch = Stopwatch.createStarted();
    LOG.trace("Mining a new block with timestamp {}", newBlockTimestamp);
    final Block block = minerBlockCreator.createBlock(newBlockTimestamp);
    LOG.trace(
        "Block created, importing to local chain, block includes {} transactions",
        block.getBody().getTransactions().size());

    final BlockImporter<C> importer =
        protocolSchedule.getByBlockNumber(block.getHeader().getNumber()).getBlockImporter();
    final boolean blockImported =
        importer.importBlock(protocolContext, block, HeaderValidationMode.FULL);
    if (blockImported) {
      notifyNewBlockListeners(block);
      final double taskTimeInSec = stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
      System.out.println(
          String.format(
              "%sbesu%s => mined block %s#%,d%s / %d tx / %,d (%01.1f%%) gas / (%s)",
              ConsoleColors.GREEN,
              ConsoleColors.RESET,
              ConsoleColors.BLUE_UNDERLINED,
              block.getHeader().getNumber(),
              ConsoleColors.RESET,
              block.getBody().getTransactions().size(),
              block.getHeader().getGasUsed(),
              (block.getHeader().getGasUsed() * 100.0) / block.getHeader().getGasLimit(),
              block.getHash().toHexString().substring(0, 4).concat("****")));
      if (block.getHeader().getNumber() > 9) {
        /*String suffix = "";
        if (lastBaseFee != null) {
          if (lastBaseFee < block.getHeader().getBaseFee().orElseThrow()) {
            suffix = "(increased because network demand is above target gas usage)";
          } else if (lastBaseFee > block.getHeader().getBaseFee().orElseThrow()) {
            suffix = "(decreased because network demand is below target gas usage)";
          }
        }
        System.out.println(
            String.format(
                "%sBASEFEE%s:%s %s",
                ConsoleColors.BLUE_UNDERLINED,
                ConsoleColors.RESET,
                block.getHeader().getBaseFee().map(bf -> Long.toString(bf)).orElseThrow(),
                suffix));
        lastBaseFee = block.getHeader().getBaseFee().orElseThrow();*/
      }
      block
          .getBody()
          .getTransactions()
          .forEach(
              tx -> {
                if (tx.isEIP1559Transaction()) {
                  System.out.printf(
                      "%s%s[EIP-1559]%s TX => value: %d %sgasPremium%s: %s %sfeeCap%s:%s%s\n",
                      ConsoleColors.WHITE_BACKGROUND_BRIGHT,
                      ConsoleColors.RED_BOLD_BRIGHT,
                      ConsoleColors.RESET,
                      tx.getValue().toLong(),
                      ConsoleColors.BLUE_UNDERLINED,
                      ConsoleColors.RESET,
                      tx.getGasPremium()
                          .map(Quantity::getValue)
                          .map(Number::longValue)
                          .map(String::valueOf)
                          .orElseThrow(),
                      ConsoleColors.BLUE_UNDERLINED,
                      ConsoleColors.RESET,
                      tx.getFeeCap()
                          .map(Quantity::getValue)
                          .map(Number::longValue)
                          .map(String::valueOf)
                          .orElseThrow(),
                      ConsoleColors.RESET);
                } else {
                  System.out.printf(
                      "%s[LEGACY]%s TX => value: %s gasPrice: %s\n",
                      ConsoleColors.BLUE_BOLD_BRIGHT,
                      ConsoleColors.RESET,
                      tx.getValue().toShortHexString(),
                      tx.getGasPrice().toShortHexString());
                }
              });
      if (block.getHeader().getNumber() == 9) {
        System.out.println(
            String.format(
                "%s%sEIP-1559 fork activation%s",
                ConsoleColors.WHITE_BACKGROUND_BRIGHT,
                ConsoleColors.RED_BOLD_BRIGHT,
                ConsoleColors.RESET));
      }
      LOG.info(
          String.format(
              "Produced and imported block #%,d / %d tx / %d om / %,d (%01.1f%%) gas / (%s) in %01.3fs",
              block.getHeader().getNumber(),
              block.getBody().getTransactions().size(),
              block.getBody().getOmmers().size(),
              block.getHeader().getGasUsed(),
              (block.getHeader().getGasUsed() * 100.0) / block.getHeader().getGasLimit(),
              block.getHash(),
              taskTimeInSec));
    } else {
      LOG.error("Illegal block mined, could not be imported to local chain.");
    }

    return blockImported;
  }

  public void cancel() {
    minerBlockCreator.cancel();
  }

  private void notifyNewBlockListeners(final Block block) {
    observers.forEach(obs -> obs.blockMined(block));
  }

  public BlockHeader getParentHeader() {
    return parentHeader;
  }
}
