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
package org.hyperledger.besu.ethereum.eth.manager;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import com.google.common.annotations.VisibleForTesting;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class ForkIdManager {

  private final Hash genesisHash;
  private final List<ForkId> forkAndHashList;

  private final Predicate<ForkId> forkIDChecker;
  private final List<Long> forks;
  private final LongSupplier chainHeadSupplier;
  private long lastHead;
  private ForkId lastComputedForkId;

  public ForkIdManager(final Blockchain blockchain, final List<Long> nonFilteredForks) {
    checkNotNull(blockchain);
    checkNotNull(nonFilteredForks);
    this.chainHeadSupplier = blockchain::getChainHeadBlockNumber;
    this.genesisHash = blockchain.getGenesisBlock().getHash();
    this.forkAndHashList = new ArrayList<>();
    this.forks =
        nonFilteredForks.stream()
            .filter(fork -> fork > 0)
            .distinct()
            .sorted()
            .collect(Collectors.toUnmodifiableList());
    if (onlyZerosForkBlocks(nonFilteredForks)) {
      this.forkIDChecker = forkId -> true;
    } else {
      this.forkIDChecker =
          createForkIDChecker(
              blockchain, genesisHash, nonFilteredForks, fs -> this.forks, forkAndHashList);
    }
  }

  public ForkId computeForkId() {
    final long head = chainHeadSupplier.getAsLong();
    if (lastHead != 0 && head == lastHead && lastComputedForkId != null) {
      return lastComputedForkId;
    }
    lastHead = head;

    final CRC32 crc = new CRC32();
    long next = 0;
    crc.update(genesisHash.toArray());
    for (final Long fork : forks) {
      if (fork <= head) {
        updateCrc(crc, fork);
        continue;
      }
      next = fork;
      break;
    }
    lastComputedForkId = new ForkId(getCurrentCrcHash(crc), next);
    return lastComputedForkId;
  }

  @VisibleForTesting
  ForkId getLatestForkId() {
    if (forkAndHashList.size() > 0) {
      return forkAndHashList.get(forkAndHashList.size() - 1);
    }
    return null;
  }

  private static Predicate<ForkId> createForkIDChecker(
      final Blockchain blockchain,
      final Hash genesisHash,
      final List<Long> forks,
      final Function<List<Long>, List<Long>> sanitizer,
      final List<ForkId> forkIds) {
    final List<Long> sanitizedForks = sanitizer.apply(forks);
    final long forkNext = createForkIds(genesisHash, sanitizedForks, forkIds);
    return eip2124(blockchain, forkNext, forkIds, highestKnownFork(sanitizedForks));
  }

  private static boolean onlyZerosForkBlocks(final List<Long> forks) {
    return forks.stream().allMatch(value -> 0L == value);
  }

  private static long highestKnownFork(final List<Long> forks) {
    return !forks.isEmpty() ? forks.get(forks.size() - 1) : 0L;
  }

  public List<ForkId> getForkAndHashList() {
    return this.forkAndHashList;
  }

  public static ForkId readFrom(final RLPInput in) {
    in.enterList();
    final Bytes hash = in.readBytes();
    final Bytes next = in.readBytes();
    in.leaveList();
    return new ForkId(hash, next);
  }

  /**
   * EIP-2124 behaviour
   *
   * @param forkId to be validated.
   * @return boolean (peer valid (true) or invalid (false))
   */
  boolean peerCheck(final ForkId forkId) {
    return forkIDChecker.test(forkId);
  }

  private static Predicate<ForkId> eip2124(
      final Blockchain blockchain,
      final long forkNext,
      final List<ForkId> forkAndHashList,
      final long highestKnownFork) {
    return forkId -> {
      if (forkId == null) {
        return true; // Another method must be used to validate (i.e. genesis hash)
      }
      // Run the fork checksum validation rule set:
      //   1. If local and remote FORK_CSUM matches, connect.
      //        The two nodes are in the same fork state currently. They might know
      //        of differing future forks, but that's not relevant until the fork
      //        triggers (might be postponed, nodes might be updated to match).
      //   2. If the remote FORK_CSUM is a subset of the local past forks and the
      //      remote FORK_NEXT matches with the locally following fork block number,
      //      connect.
      //        Remote node is currently syncing. It might eventually diverge from
      //        us, but at this current point in time we don't have enough information.
      //   3. If the remote FORK_CSUM is a superset of the local past forks and can
      //      be completed with locally known future forks, connect.
      //        Local node is currently syncing. It might eventually diverge from
      //        the remote, but at this current point in time we don't have enough
      //        information.
      //   4. Reject in all other cases.
      if (isHashKnown(forkId.getHash(), forkAndHashList)) {
        if (blockchain.getChainHeadBlockNumber() < forkNext) {
          return true;
        } else {
          if (isForkKnown(forkId.getNext(), highestKnownFork, forkAndHashList)) {
            return isRemoteAwareOfPresent(
                forkId.getHash(), forkId.getNext(), highestKnownFork, forkAndHashList);
          } else {
            return false;
          }
        }
      } else {
        return false;
      }
    };
  }
  /**
   * Non EIP-2124 behaviour
   *
   * @param peerGenesisHash Hash to be validated.
   * @return boolean
   */
  public boolean peerCheck(final Bytes32 peerGenesisHash) {
    return !peerGenesisHash.equals(genesisHash);
  }

  private static boolean isHashKnown(final Bytes forkHash, final List<ForkId> forkAndHashList) {
    return forkAndHashList.stream().map(ForkId::getHash).anyMatch(hash -> hash.equals(forkHash));
  }

  private static boolean isForkKnown(
      final Long nextFork, final long highestKnownFork, final List<ForkId> forkAndHashList) {
    return highestKnownFork < nextFork
        || forkAndHashList.stream().map(ForkId::getNext).anyMatch(fork -> fork.equals(nextFork));
  }

  private static boolean isRemoteAwareOfPresent(
      final Bytes forkHash,
      final Long nextFork,
      final long highestKnownFork,
      final List<ForkId> forkAndHashList) {
    for (final ForkId j : forkAndHashList) {
      if (forkHash.equals(j.getHash())) {
        if (nextFork.equals(j.getNext())) {
          return true;
        } else if (j.getNext() == 0L) {
          return highestKnownFork <= nextFork; // Remote aware of an additional future fork
        } else {
          return false;
        }
      }
    }
    return false;
  }

  private static long createForkIds(
      final Hash genesisHash, final List<Long> forks, final List<ForkId> forkIds) {
    final CRC32 crc = new CRC32();
    crc.update(genesisHash.toArray());
    final List<Bytes> forkHashes = new ArrayList<>(List.of(getCurrentCrcHash(crc)));
    forks.forEach(
        fork -> {
          updateCrc(crc, fork);
          forkHashes.add(getCurrentCrcHash(crc));
        });

    // This loop is for all the fork hashes that have an associated "next fork"
    for (int i = 0; i < forks.size(); i++) {
      forkIds.add(new ForkId(forkHashes.get(i), forks.get(i)));
    }
    long forkNext = 0;
    if (!forks.isEmpty()) {
      forkNext = forkIds.get(forkIds.size() - 1).getNext();
      forkIds.add(new ForkId(forkHashes.get(forkHashes.size() - 1), 0));
    }
    return forkNext;
  }

  private static void updateCrc(final CRC32 crc, final Long block) {
    final byte[] byteRepresentationFork = longToBigEndian(block);
    crc.update(byteRepresentationFork, 0, byteRepresentationFork.length);
  }

  private static Bytes getCurrentCrcHash(final CRC32 crc) {
    return Bytes.ofUnsignedInt(crc.getValue());
  }

  public static class ForkId {
    final Bytes hash;
    final Bytes next;
    Bytes forkIdRLP;

    private ForkId(final Bytes hash, final Bytes next) {
      this.hash = hash;
      this.next = next;
      createForkIdRLP();
    }

    public ForkId(final Bytes hash, final long next) {
      this(hash, Bytes.wrap(longToBigEndian(next)).trimLeadingZeros());
    }

    public long getNext() {
      return next.toLong();
    }

    public Bytes getHash() {
      return hash;
    }

    void createForkIdRLP() {
      final BytesValueRLPOutput out = new BytesValueRLPOutput();
      writeTo(out);
      forkIdRLP = out.encoded();
    }

    public void writeTo(final RLPOutput out) {
      out.startList();
      out.writeBytes(hash);
      out.writeBytes(next);
      out.endList();
    }

    public static ForkId readFrom(final RLPInput in) {
      in.enterList();
      final Bytes hash = in.readBytes();
      final long next = in.readLongScalar();
      in.leaveList();
      return new ForkId(hash, next);
    }

    public List<ForkId> asList() {
      final ArrayList<ForkId> forRLP = new ArrayList<>();
      forRLP.add(this);
      return forRLP;
    }

    @Override
    public String toString() {
      return "ForkId(hash=" + this.hash + ", next=" + next.toLong() + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof ForkId) {
        final ForkId other = (ForkId) obj;
        final long thisNext = next.toLong();
        return other.getHash().equals(this.hash) && thisNext == other.getNext();
      }
      return false;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

  // next two methods adopted from:
  // https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/util/Pack.java
  private static byte[] longToBigEndian(final long n) {
    final byte[] bs = new byte[8];
    intToBigEndian((int) (n >>> 32), bs, 0);
    intToBigEndian((int) (n & 0xffffffffL), bs, 4);
    return bs;
  }

  @SuppressWarnings("MethodInputParametersMustBeFinal")
  private static void intToBigEndian(final int n, final byte[] bs, int off) {
    bs[off] = (byte) (n >>> 24);
    bs[++off] = (byte) (n >>> 16);
    bs[++off] = (byte) (n >>> 8);
    bs[++off] = (byte) (n);
  }
}
