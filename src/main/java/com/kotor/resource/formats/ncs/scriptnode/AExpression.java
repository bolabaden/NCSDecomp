// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;

public interface AExpression {
   @Override
   String toString();

   ScriptNode parent();

   void parent(ScriptNode var1);

   StackEntry stackentry();

   void stackentry(StackEntry var1);
}

