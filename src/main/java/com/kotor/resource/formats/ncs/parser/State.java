// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.parser;

import com.kotor.resource.formats.ncs.node.Node;

/**
 * Lightweight stack frame used by the generated parser shift/reduce engine.
 */
final class State {
   int state;
   Node node;

   State(int state, Node node) {
      this.state = state;
      this.node = node;
   }
}

