// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bytecode-only round-trip regression:
 * NSS -> NCS -> NSS -> NCS, comparing only bytecode/offset/p-code between
 * the first and second NCS outputs (steps 2 vs 4).
 * Fast-fails on the first mismatch with detailed diagnostics.
 */
public class NCSDecompCLIBytecodeRoundTripTest {

   public static void main(String[] args) {
      NCSDecompCLIRoundTripTest runner = new NCSDecompCLIRoundTripTest();
      int exitCode = runner.runRoundTripBytecodeSuite();
      if (exitCode != 0) {
         System.exit(exitCode);
      }
   }

   @Test
   public void testBytecodeOnlyRoundTripSuite() {
      NCSDecompCLIRoundTripTest runner = new NCSDecompCLIRoundTripTest();
      int exitCode = runner.runRoundTripBytecodeSuite();
      assertEquals(0, exitCode, "Bytecode round-trip suite should pass with exit code 0");
   }
}

