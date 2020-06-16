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
package org.hyperledger.besu.ethereum.api.handlers;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class IsAliveHandler implements Supplier<Boolean> {

  private final AtomicBoolean alive;

  public IsAliveHandler(final boolean alive) {
    this(new AtomicBoolean(alive));
  }

  public IsAliveHandler(final AtomicBoolean alive) {
    this.alive = alive;
  }

  public IsAliveHandler(final long timeoutSec) {
    this(new AtomicBoolean(true), timeoutSec);
  }

  public IsAliveHandler(final AtomicBoolean alive, final long timeoutSec) {
    this.alive = alive;
    Executors.newSingleThreadScheduledExecutor()
        .schedule(() -> alive.set(false), timeoutSec, TimeUnit.SECONDS);
  }

  @Override
  public Boolean get() {
    return alive.get();
  }
}
