// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import java.io.Serializable;

/**
 * Hook used by TypedLinkedList to enforce parent/ownership rules on insert.
 */
public interface Cast<T> extends Serializable {
   T cast(Object var1);
}

