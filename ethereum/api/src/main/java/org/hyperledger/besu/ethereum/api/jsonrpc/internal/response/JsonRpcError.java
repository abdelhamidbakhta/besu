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
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum JsonRpcError {
  // Standard errors
  PARSE_ERROR(-32700, "Parse error"),
  INVALID_REQUEST(-32600, "Invalid Request"),
  METHOD_NOT_FOUND(-32601, "Method not found"),
  INVALID_PARAMS(-32602, "Invalid params"),
  INTERNAL_ERROR(-32603, "Internal error"),
  TIMEOUT_ERROR(-32603, "Timeout expired"),

  METHOD_NOT_ENABLED(-32604, "Method not enabled"),

  // eth_sendTransaction specific error message
  ETH_SEND_TX_NOT_AVAILABLE(
      -32604,
      "The method eth_sendTransaction is not supported. Use eth_sendRawTransaction to send a signed transaction to Besu."),
  ETH_SEND_TX_ALREADY_KNOWN(-32000, "Known transaction"),
  ETH_SEND_TX_REPLACEMENT_UNDERPRICED(-32000, "Replacement transaction underpriced"),
  // P2P related errors
  P2P_DISABLED(-32000, "P2P has been disabled. This functionality is not available"),
  P2P_NETWORK_NOT_RUNNING(-32000, "P2P network is not running"),

  // Filter & Subscription Errors
  FILTER_NOT_FOUND(-32000, "Filter not found"),
  LOGS_FILTER_NOT_FOUND(-32000, "Logs filter not found"),
  SUBSCRIPTION_NOT_FOUND(-32000, "Subscription not found"),
  NO_MINING_WORK_FOUND(-32000, "No mining work available yet"),

  // Transaction validation failures
  NONCE_TOO_LOW(-32001, "Nonce too low"),
  INVALID_TRANSACTION_SIGNATURE(-32002, "Invalid signature"),
  INTRINSIC_GAS_EXCEEDS_LIMIT(-32003, "Intrinsic gas exceeds gas limit"),
  TRANSACTION_UPFRONT_COST_EXCEEDS_BALANCE(-32004, "Upfront cost exceeds account balance"),
  EXCEEDS_BLOCK_GAS_LIMIT(-32005, "Transaction gas limit exceeds block gas limit"),
  INCORRECT_NONCE(-32006, "Incorrect nonce"),
  TX_SENDER_NOT_AUTHORIZED(-32007, "Sender account not authorized to send transactions"),
  CHAIN_HEAD_WORLD_STATE_NOT_AVAILABLE(-32008, "Initial sync is still in progress"),
  GAS_PRICE_TOO_LOW(-32009, "Gas price below configured minimum gas price"),
  WRONG_CHAIN_ID(-32000, "Wrong chainId"),
  REPLAY_PROTECTED_SIGNATURES_NOT_SUPPORTED(-32000, "ChainId not supported"),
  TX_FEECAP_EXCEEDED(-32000, "Transaction fee cap exceeded"),

  // Miner failures
  COINBASE_NOT_SET(-32010, "Coinbase not set. Unable to start mining without a coinbase"),
  NO_HASHES_PER_SECOND(-32011, "No hashes being generated by the current node"),

  // Wallet errors
  COINBASE_NOT_SPECIFIED(-32000, "Coinbase must be explicitly specified"),

  // Account errors
  NO_ACCOUNT_FOUND(-32000, "Account not found"),

  // Worldstate errors
  WORLD_STATE_UNAVAILABLE(-32000, "World state unavailable"),

  // Debug failures
  PARENT_BLOCK_NOT_FOUND(-32000, "Parent block not found"),

  // Permissioning/Account allowlist errors
  ACCOUNT_ALLOWLIST_NOT_ENABLED(-32000, "Account allowlist has not been enabled"),
  ACCOUNT_ALLOWLIST_EMPTY_ENTRY(-32000, "Request contains an empty list of accounts"),
  ACCOUNT_ALLOWLIST_INVALID_ENTRY(-32000, "Request contains an invalid account"),
  ACCOUNT_ALLOWLIST_DUPLICATED_ENTRY(-32000, "Request contains duplicate accounts"),
  ACCOUNT_ALLOWLIST_EXISTING_ENTRY(-32000, "Cannot add an existing account to allowlist"),
  ACCOUNT_ALLOWLIST_ABSENT_ENTRY(-32000, "Cannot remove an absent account from allowlist"),

  // Permissioning/Node allowlist errors
  NODE_ALLOWLIST_NOT_ENABLED(-32000, "Node allowlist has not been enabled"),
  NODE_ALLOWLIST_EMPTY_ENTRY(-32000, "Request contains an empty list of nodes"),
  NODE_ALLOWLIST_INVALID_ENTRY(-32000, "Request contains an invalid node"),
  NODE_ALLOWLIST_DUPLICATED_ENTRY(-32000, "Request contains duplicate nodes"),
  NODE_ALLOWLIST_EXISTING_ENTRY(-32000, "Cannot add an existing node to allowlist"),
  NODE_ALLOWLIST_MISSING_ENTRY(-32000, "Cannot remove an absent node from allowlist"),
  NODE_ALLOWLIST_FIXED_NODE_CANNOT_BE_REMOVED(
      -32000, "Cannot remove a fixed node (bootnode or static node) from allowlist"),

  // Permissioning/persistence errors
  ALLOWLIST_PERSIST_FAILURE(
      -32000, "Unable to persist changes to allowlist configuration file. Changes reverted"),
  ALLOWLIST_FILE_SYNC(
      -32000,
      "The permissioning allowlist configuration file is out of sync.  The changes have been applied, but not persisted to disk"),
  ALLOWLIST_RELOAD_ERROR(
      -32000,
      "Error reloading permissions file. Please use perm_getAccountsAllowlist and perm_getNodesAllowlist to review the current state of the allowlists"),
  PERMISSIONING_NOT_ENABLED(-32000, "Node/Account allowlist has not been enabled"),
  NON_PERMITTED_NODE_CANNOT_BE_ADDED_AS_A_PEER(-32000, "Cannot add a non-permitted node as a peer"),

  // Permissioning/Authorization errors
  UNAUTHORIZED(-40100, "Unauthorized"),

  // Private transaction errors
  ENCLAVE_ERROR(-50100, "Error communicating with enclave"),
  UNIMPLEMENTED_PRIVATE_TRANSACTION_TYPE(-50100, "Unimplemented private transaction type"),
  PRIVACY_NOT_ENABLED(-50100, "Privacy is not enabled"),
  CREATE_PRIVACY_GROUP_ERROR(-50100, "Error creating privacy group"),
  DELETE_PRIVACY_GROUP_ERROR(-50100, "Error deleting privacy group"),
  FIND_PRIVACY_GROUP_ERROR(-50100, "Error finding privacy group"),
  FIND_ONCHAIN_PRIVACY_GROUP_ERROR(-50100, "Error finding onchain privacy group"),
  VALUE_NOT_ZERO(-50100, "We cannot transfer ether in a private transaction yet."),
  DECODE_ERROR(-50100, "Unable to decode the private signed raw transaction"),
  GET_PRIVATE_TRANSACTION_NONCE_ERROR(-50100, "Unable to determine nonce for account in group."),
  OFFCHAIN_PRIVACY_GROUP_DOES_NOT_EXIST(-50100, "Offchain Privacy group does not exist."),
  ONCHAIN_PRIVACY_GROUP_DOES_NOT_EXIST(-50100, "Onchain Privacy group does not exist."),
  ONCHAIN_PRIVACY_GROUP_NOT_ENABLED(-50100, "Onchain privacy groups not enabled."),
  OFFCHAIN_PRIVACY_GROUP_NOT_ENABLED(
      -50100, "Offchain privacy group can't be used with Onchain privacy groups enabled."),
  ONCHAIN_PRIVACY_GROUP_ID_NOT_AVAILABLE(
      -50100, "Private transactions to onchain privacy groups must use privacyGroupId"),
  PRIVATE_FROM_DOES_NOT_MATCH_ENCLAVE_PUBLIC_KEY(
      -50100, "Private from does not match enclave public key"),
  PMT_FAILED_INTRINSIC_GAS_EXCEEDS_LIMIT(
      -50100,
      "Private Marker Transaction failed due to intrinsic gas exceeding the limit. Gas limit used from the Private Transaction."),

  CANT_CONNECT_TO_LOCAL_PEER(-32100, "Cannot add local node as peer."),

  // Invalid input errors
  ENODE_ID_INVALID(
      -32000,
      "Invalid node ID: node ID must have exactly 128 hexadecimal characters and should not include any '0x' hex prefix."),

  // Enclave errors
  NODE_MISSING_PEER_URL(-50200, "NodeMissingPeerUrl"),
  NODE_PUSHING_TO_PEER(-50200, "NodePushingToPeer"),
  NODE_PROPAGATING_TO_ALL_PEERS(-50200, "NodePropagatingToAllPeers"),
  NO_SENDER_KEY(-50200, "NoSenderKey"),
  INVALID_PAYLOAD(-50200, "InvalidPayload"),
  ENCLAVE_CREATE_KEY_PAIR(-50200, "EnclaveCreateKeyPair"),
  ENCLAVE_DECODE_PUBLIC_KEY(-50200, "EnclaveDecodePublicKey"),
  ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY(-50200, "EnclaveDecryptWrongPrivateKey"),
  ENCLAVE_ENCRYPT_COMBINE_KEYS(-50200, "EnclaveEncryptCombineKeys"),
  ENCLAVE_MISSING_PRIVATE_KEY_PASSWORD(-50200, "EnclaveMissingPrivateKeyPasswords"),
  ENCLAVE_NO_MATCHING_PRIVATE_KEY(-50200, "EnclaveNoMatchingPrivateKey"),
  ENCLAVE_NOT_PAYLOAD_OWNER(-50200, "EnclaveNotPayloadOwner"),
  ENCLAVE_UNSUPPORTED_PRIVATE_KEY_TYPE(-50200, "EnclaveUnsupportedPrivateKeyType"),
  ENCLAVE_STORAGE_DECRYPT(-50200, "EnclaveStorageDecrypt"),
  ENCLAVE_PRIVACY_GROUP_CREATION(-50200, "EnclavePrivacyGroupIdCreation"),
  ENCLAVE_PAYLOAD_NOT_FOUND(-50200, "EnclavePayloadNotFound"),
  CREATE_GROUP_INCLUDE_SELF(-50200, "CreatePrivacyGroupShouldIncludeSelf"),

  /** Storing privacy group issue */
  ENCLAVE_UNABLE_STORE_PRIVACY_GROUP(-50200, "PrivacyGroupNotStored"),
  ENCLAVE_UNABLE_DELETE_PRIVACY_GROUP(-50200, "PrivacyGroupNotDeleted"),
  ENCLAVE_UNABLE_PUSH_DELETE_PRIVACY_GROUP(-50200, "PrivacyGroupNotPushed"),
  ENCLAVE_PRIVACY_GROUP_MISSING(-50200, "PrivacyGroupNotFound"),
  ENCLAVE_PRIVACY_QUERY_ERROR(-50200, "PrivacyGroupQueryError"),
  ENCLAVE_KEYS_CANNOT_DECRYPT_PAYLOAD(-50200, "EnclaveKeysCannotDecryptPayload"),
  METHOD_UNIMPLEMENTED(-50200, "MethodUnimplemented"),

  /** Plugins error */
  PLUGIN_NOT_FOUND(-60000, "Plugin not found"),

  // Retesteth Errors

  BLOCK_RLP_IMPORT_ERROR(-32000, "Could not decode RLP for Block"),
  BLOCK_IMPORT_ERROR(-32000, "Could not import Block");

  private final int code;
  private final String message;

  JsonRpcError(final int code, final String message) {
    this.code = code;
    this.message = message;
  }

  @JsonGetter("code")
  public int getCode() {
    return code;
  }

  @JsonGetter("message")
  public String getMessage() {
    return message;
  }

  @JsonCreator
  public static JsonRpcError fromJson(
      @JsonProperty("code") final int code, @JsonProperty("message") final String message) {
    for (final JsonRpcError error : JsonRpcError.values()) {
      if (error.code == code && error.message.equals(message)) {
        return error;
      }
    }
    return null;
  }
}
