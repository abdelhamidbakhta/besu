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
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods;

import org.hyperledger.besu.ethereum.api.cache.TracingCacheManager;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.parameters.TraceTypeParameter;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.parameters.TraceTypeParameter.TraceType;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockTrace;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.BlockTracer;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.TraceFormatter;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.TraceWriter;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.diff.StateDiffGenerator;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTraceGenerator;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.vm.VmTraceGenerator;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.debug.TraceOptions;
import org.hyperledger.besu.ethereum.mainnet.TransactionProcessor.Result;
import org.hyperledger.besu.ethereum.vm.DebugOperationTracer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Suppliers;

public class TraceReplayBlockTransactions extends AbstractBlockParameterMethod {
  private final Supplier<BlockTracer> blockTracerSupplier;
  private final Supplier<StateDiffGenerator> stateDiffGenerator =
      Suppliers.memoize(StateDiffGenerator::new);
  private final Optional<TracingCacheManager> tracingCacheManager;

  public TraceReplayBlockTransactions(
      final Supplier<BlockTracer> blockTracerSupplier,
      final BlockchainQueries queries,
      final Optional<TracingCacheManager> tracingCacheManager) {
    super(queries);
    this.blockTracerSupplier = blockTracerSupplier;
    this.tracingCacheManager = tracingCacheManager;
  }

  @Override
  public String getName() {
    return RpcMethod.TRACE_REPLAY_BLOCK_TRANSACTIONS.getMethodName();
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequestContext request) {
    return request.getRequiredParameter(0, BlockParameter.class);
  }

  @Override
  protected Object resultByBlockNumber(
      final JsonRpcRequestContext request, final long blockNumber) {
    final TraceTypeParameter traceTypeParameter =
        request.getRequiredParameter(1, TraceTypeParameter.class);

    if (blockNumber == BlockHeader.GENESIS_BLOCK_NUMBER) {
      // Nothing to trace for the genesis block
      return emptyResult();
    }
    return tracingCacheManager
        .flatMap(tracingCacheManager -> tracingCacheManager.blockTraceAt(blockNumber))
        .map(blockTrace -> traceBlock(blockTrace, traceTypeParameter))
        .orElse(
            getBlockchainQueries()
                .getBlockchain()
                .getBlockByNumber(blockNumber)
                .map((block) -> traceBlock(block, traceTypeParameter))
                .orElse(null));
  }

  private Object traceBlock(
      final BlockTrace blockTrace, final TraceTypeParameter traceTypeParameter) {
    if (blockTrace == null) {
      return emptyResult();
    }
    return generateTracesFromTransactionTrace(
        blockTrace.getTransactionTraces(), traceTypeParameter);
  }

  private Object traceBlock(final Block block, final TraceTypeParameter traceTypeParameter) {
    if (block == null || block.getBody().getTransactions().isEmpty()) {
      return emptyResult();
    }
    // TODO: generate options based on traceTypeParameter
    final TraceOptions traceOptions = TraceOptions.DEFAULT;
    return tracingCacheManager
        .flatMap(
            tracingCacheManager -> tracingCacheManager.blockTraceAt(block.getHeader().getNumber()))
        .or(() -> blockTracerSupplier.get().trace(block, new DebugOperationTracer(traceOptions)))
        .map(BlockTrace::getTransactionTraces)
        .map((traces) -> generateTracesFromTransactionTrace(traces, traceTypeParameter))
        .orElse(null);
  }

  private JsonNode generateTracesFromTransactionTrace(
      final List<TransactionTrace> transactionTraces, final TraceTypeParameter traceTypeParameter) {
    final Set<TraceTypeParameter.TraceType> traceTypes = traceTypeParameter.getTraceTypes();
    final ObjectMapper mapper = new ObjectMapper();
    final ArrayNode resultArrayNode = mapper.createArrayNode();
    final AtomicInteger traceCounter = new AtomicInteger(0);
    transactionTraces.forEach(
        transactionTrace ->
            handleTransactionTrace(
                transactionTrace, traceTypes, mapper, resultArrayNode, traceCounter));
    return resultArrayNode;
  }

  private void handleTransactionTrace(
      final TransactionTrace transactionTrace,
      final Set<TraceTypeParameter.TraceType> traceTypes,
      final ObjectMapper mapper,
      final ArrayNode resultArrayNode,
      final AtomicInteger traceCounter) {
    final ObjectNode resultNode = mapper.createObjectNode();

    Result result = transactionTrace.getResult();
    resultNode.put("output", result.getRevertReason().orElse(result.getOutput()).toString());

    if (traceTypes.contains(TraceType.STATE_DIFF)) {
      generateTracesFromTransactionTrace(
          trace -> resultNode.putPOJO("stateDiff", trace),
          transactionTrace,
          (txTrace, ignored) -> stateDiffGenerator.get().generateStateDiff(txTrace),
          traceCounter);
    }
    setNullNodesIfNotPresent(resultNode, "stateDiff");
    if (traceTypes.contains(TraceTypeParameter.TraceType.TRACE)) {
      generateTracesFromTransactionTrace(
          resultNode.putArray("trace")::addPOJO,
          transactionTrace,
          FlatTraceGenerator::generateFromTransactionTrace,
          traceCounter);
    }
    setEmptyArrayIfNotPresent(resultNode, "trace");
    resultNode.put("transactionHash", transactionTrace.getTransaction().getHash().toHexString());
    if (traceTypes.contains(TraceTypeParameter.TraceType.VM_TRACE)) {
      generateTracesFromTransactionTrace(
          trace -> resultNode.putPOJO("vmTrace", trace),
          transactionTrace,
          (__, ignored) -> new VmTraceGenerator(transactionTrace).generateTraceStream(),
          traceCounter);
    }
    setNullNodesIfNotPresent(resultNode, "vmTrace");
    resultArrayNode.add(resultNode);
  }

  private void generateTracesFromTransactionTrace(
      final TraceWriter writer,
      final TransactionTrace transactionTrace,
      final TraceFormatter formatter,
      final AtomicInteger traceCounter) {
    formatter.format(transactionTrace, traceCounter).forEachOrdered(writer::write);
  }

  private void setNullNodesIfNotPresent(final ObjectNode parentNode, final String... keys) {
    Arrays.asList(keys)
        .forEach(
            key ->
                Optional.ofNullable(parentNode.get(key))
                    .ifPresentOrElse(ignored -> {}, () -> parentNode.put(key, (String) null)));
  }

  private void setEmptyArrayIfNotPresent(final ObjectNode parentNode, final String... keys) {
    Arrays.asList(keys)
        .forEach(
            key ->
                Optional.ofNullable(parentNode.get(key))
                    .ifPresentOrElse(ignored -> {}, () -> parentNode.putArray(key)));
  }

  private Object emptyResult() {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.createArrayNode();
  }
}
