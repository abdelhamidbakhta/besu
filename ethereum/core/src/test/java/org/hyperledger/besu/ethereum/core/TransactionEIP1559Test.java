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
package org.hyperledger.besu.ethereum.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hyperledger.besu.config.experimental.ExperimentalEIPs;
import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.SignatureAlgorithm;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.After;
import org.junit.Test;

public class TransactionEIP1559Test {
  private static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);

  private static final BigInteger CHAIN_ID = BigInteger.valueOf(7822);
  private static final String PRIVATE_KEY_STR = "0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";
  private static final KeyPair PRIVATE_KEY =  keyPair(PRIVATE_KEY_STR);
  private static final List<AccessListEntry> ACCESS_LIST =
          List.of(
                  new AccessListEntry(
                          Address.fromHexString("0x000000000000000000000000000000000000aaaa"),
                          List.of(Bytes32.ZERO)));
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  @After
  public void reset() {
    ExperimentalEIPs.eip1559Enabled = ExperimentalEIPs.EIP1559_ENABLED_DEFAULT_VALUE;
  }

  @Test
  public void buildEip1559Transaction() {
    final Transaction tx =
        Transaction.builder()
            .chainId(CHAIN_ID)
            .nonce(0)
            .value(Wei.ZERO)
            .gasLimit(30000)
            .gasPremium(Wei.of(2))
            .payload(Bytes.EMPTY.trimLeadingZeros())
            .feeCap(Wei.of(new BigInteger("5000000000", 10)))
            .gasPrice(null)
            .to(Address.fromHexString("0x000000000000000000000000000000000000aaaa"))
            .accessList(ACCESS_LIST)
            .guessType()
            .signAndBuild(
                keyPair("0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63"));
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    tx.writeTo(out);
    System.out.println(out.encoded().toHexString());
    System.out.println(tx.getUpfrontCost());
    // final String raw =
    // "b8a902f8a686796f6c6f7632800285012a05f20082753094000000000000000000000000000000000000aaaa8080f838f794000000000000000000000000000000000000aaaae1a0000000000000000000000000000000000000000000000000000000000000000001a00c1d69648e348fe26155b45de45004f0e4195f6352d8f0935bc93e98a3e2a862a060064e5b9765c0ac74223b0cf49635c59ae0faf82044fd17bcc68a549ade6f95";
    final String raw = out.encoded().toHexString();
    final Transaction decoded = Transaction.readFrom(RLP.input(Bytes.fromHexString(raw)));
    System.out.println(decoded);
    System.out.println(decoded.getAccessList().orElseThrow().get(0).getAddress().toHexString());
    System.out.println(decoded.getAccessList().orElseThrow().get(0).getStorageKeys());
  }

  @Test
  public void encoding(){
    final List<TestCase> testCases = new ArrayList<>();
    final int cases = 10;
    for (int i = 0; i < cases; i++) {
      final BytesValueRLPOutput out = new BytesValueRLPOutput();
      final Transaction tx = genTx();
      tx.writeTo(out);
      final String raw = out.encoded().toHexString();
      testCases.add(new TestCase(tx, PRIVATE_KEY_STR, raw));
    }

    System.out.println(GSON.toJson(testCases));
  }

  private static Transaction genTx(){
    return tx(rand(1, 1000), rand(1, 100000000), rand(20000, 100000), rand(1, 90000), rand(80000, 150000));
  }

  private static long rand(final long min, final long max){
    return (long) ((Math.random() * (max - min)) + min);
  }

  private static Transaction tx(final long nonce, final long value, final long gasLimit, final long maxInclusionFeePerGas, final long maxFeePerGas){
    return Transaction.builder()
            .chainId(CHAIN_ID)
            .nonce(nonce)
            .value(Wei.of(value))
            .gasLimit(gasLimit)
            .gasPremium(Wei.of(maxInclusionFeePerGas))
            .feeCap(Wei.of(maxFeePerGas))
            .payload(Bytes.EMPTY.trimLeadingZeros())
            .gasPrice(null)
            .to(Address.fromHexString("0x000000000000000000000000000000000000aaaa"))
            .guessType()
            .signAndBuild(
                    keyPair("0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63"));
  }

  static class TestCase {
    long nonce;
    long value;
    long gasLimit;
    long maxInclusionFeePerGas;
    long maxFeePerGas;
    String to;
    String privateKey;
    String signedTransactionRLP;

    public TestCase(final Transaction tx, final String privateKey, final String signedTransactionRLP) {
      this.nonce = tx.getNonce();
      this.value = tx.getValue().toLong();
      this.gasLimit = tx.getGasLimit();
      this.maxInclusionFeePerGas = tx.getGasPremium().orElseThrow().getValue().longValue();
      this.maxFeePerGas = tx.getFeeCap().orElseThrow().getValue().longValue();
      this.to = tx.getTo().orElseThrow().toHexString();
      this.privateKey = privateKey;
      this.signedTransactionRLP = signedTransactionRLP;
    }

    public void setNonce(final long nonce) {
      this.nonce = nonce;
    }

    public String getTo() {
      return to;
    }

    public void setTo(final String to) {
      this.to = to;
    }

    public String getPrivateKey() {
      return privateKey;
    }

    public void setPrivateKey(final String privateKey) {
      this.privateKey = privateKey;
    }

    public String getSignedTransactionRLP() {
      return signedTransactionRLP;
    }

    public void setSignedTransactionRLP(final String signedTransactionRLP) {
      this.signedTransactionRLP = signedTransactionRLP;
    }

    public long getNonce() {
      return nonce;
    }

    public void setNonce(final int nonce) {
      this.nonce = nonce;
    }

    public long getValue() {
      return value;
    }

    public void setValue(final long value) {
      this.value = value;
    }

    public long getGasLimit() {
      return gasLimit;
    }

    public void setGasLimit(final long gasLimit) {
      this.gasLimit = gasLimit;
    }

    public long getMaxInclusionFeePerGas() {
      return maxInclusionFeePerGas;
    }

    public void setMaxInclusionFeePerGas(final long maxInclusionFeePerGas) {
      this.maxInclusionFeePerGas = maxInclusionFeePerGas;
    }

    public long getMaxFeePerGas() {
      return maxFeePerGas;
    }

    public void setMaxFeePerGas(final long maxFeePerGas) {
      this.maxFeePerGas = maxFeePerGas;
    }
  }

  private static KeyPair keyPair(final String privateKey) {
    final SignatureAlgorithm signatureAlgorithm = SIGNATURE_ALGORITHM.get();
    return signatureAlgorithm.createKeyPair(
        signatureAlgorithm.createPrivateKey(Bytes32.fromHexString(privateKey)));
  }
}
