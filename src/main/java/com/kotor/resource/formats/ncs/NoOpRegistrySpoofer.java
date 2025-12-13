// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

/**
 * A no-op registry spoofer for compilers that don't require registry spoofing.
 * <p>
 * This class provides the same interface as {@link RegistrySpoofer} but performs
 * no operations. It's used to maintain polymorphism when some compilers need
 * registry spoofing and others don't.
 */
public class NoOpRegistrySpoofer implements AutoCloseable {
   /**
    * Creates a new no-op registry spoofer.
    */
   public NoOpRegistrySpoofer() {
      System.err.println("DEBUG NoOpRegistrySpoofer: Created (no registry spoofing needed)");
   }

   /**
    * No-op activation.
    *
    * @return this instance
    */
   public NoOpRegistrySpoofer activate() {
      System.err.println("DEBUG NoOpRegistrySpoofer: activate() called (no-op)");
      return this;
   }

   /**
    * No-op close.
    */
   @Override
   public void close() {
      System.err.println("DEBUG NoOpRegistrySpoofer: close() called (no-op)");
   }
}
