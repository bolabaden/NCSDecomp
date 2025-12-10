// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.analysis;

import com.kotor.resource.formats.ncs.ActionsData;
import com.kotor.resource.formats.ncs.DoTypes;
import com.kotor.resource.formats.ncs.node.AMoveSpCommand;
import com.kotor.resource.formats.ncs.utils.NodeAnalysisData;
import com.kotor.resource.formats.ncs.utils.NodeUtils;
import com.kotor.resource.formats.ncs.utils.SubroutineAnalysisData;
import com.kotor.resource.formats.ncs.utils.SubroutinePathFinder;
import com.kotor.resource.formats.ncs.utils.SubroutineState;
import com.kotor.resource.formats.ncs.utils.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs subroutine prototyping in a deterministic, graph-driven order.
 */
public class PrototypeEngine {
   private static final int MAX_PASSES = 3;
   private final NodeAnalysisData nodedata;
   private final SubroutineAnalysisData subdata;
   private final ActionsData actions;
   private final boolean strict;

   public PrototypeEngine(NodeAnalysisData nodedata, SubroutineAnalysisData subdata, ActionsData actions, boolean strict) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.actions = actions;
      this.strict = strict;
   }

   public void run() {
      CallGraphBuilder.CallGraph graph = new CallGraphBuilder(this.nodedata, this.subdata).build();
      Map<Integer, com.kotor.resource.formats.ncs.node.ASubroutine> subByPos = this.indexSubroutines();

      int mainPos = this.nodedata.getPos(this.subdata.getMainSub());
      Set<Integer> reachable = graph.reachableFrom(mainPos);
      if (this.subdata.getGlobalsSub() != null) {
         reachable.addAll(graph.reachableFrom(this.nodedata.getPos(this.subdata.getGlobalsSub())));
      }

      List<Set<Integer>> sccs = SCCUtil.compute(graph.edges());
      for (Set<Integer> scc : sccs) {
         boolean containsReachable = scc.stream().anyMatch(reachable::contains);
         if (!containsReachable) {
            continue;
         }
         this.prototypeComponent(scc, subByPos);
      }

      Map<Integer, Integer> callsiteParams = new CallSiteAnalyzer(this.nodedata, this.subdata, this.actions).analyze();
      this.ensureAllPrototyped(subByPos.values(), callsiteParams);
   }

   private Map<Integer, com.kotor.resource.formats.ncs.node.ASubroutine> indexSubroutines() {
      Map<Integer, com.kotor.resource.formats.ncs.node.ASubroutine> map = new HashMap<>();
      Iterator<com.kotor.resource.formats.ncs.node.ASubroutine> it = this.subdata.getSubroutines();
      while (it.hasNext()) {
         com.kotor.resource.formats.ncs.node.ASubroutine sub = it.next();
         map.put(this.nodedata.getPos(sub), sub);
      }
      return map;
   }

   private void prototypeComponent(Set<Integer> component, Map<Integer, com.kotor.resource.formats.ncs.node.ASubroutine> subByPos) {
      List<com.kotor.resource.formats.ncs.node.ASubroutine> subs = new ArrayList<>();
      for (int pos : component) {
         com.kotor.resource.formats.ncs.node.ASubroutine sub = subByPos.get(pos);
         if (sub != null) {
            subs.add(sub);
         }
      }

      for (int pass = 0; pass < MAX_PASSES; pass++) {
         boolean progress = false;
         for (com.kotor.resource.formats.ncs.node.ASubroutine sub : subs) {
            SubroutineState state = this.subdata.getState(sub);
            if (state.isPrototyped()) {
               continue;
            }

            sub.apply(new SubroutinePathFinder(state, this.nodedata, this.subdata, pass));
            if (state.isBeingPrototyped()) {
               DoTypes dotypes = new DoTypes(state, this.nodedata, this.subdata, this.actions, true);
               sub.apply(dotypes);
               dotypes.done();
               progress = progress || state.isPrototyped();
            }
         }
         if (!progress) {
            break;
         }
      }
   }

   private void ensureAllPrototyped(
      Iterable<com.kotor.resource.formats.ncs.node.ASubroutine> subs,
      Map<Integer, Integer> callsiteParams
   ) {
      for (com.kotor.resource.formats.ncs.node.ASubroutine sub : subs) {
         SubroutineState state = this.subdata.getState(sub);
         if (!state.isPrototyped()) {
            if (this.strict) {
               System.out.println(
                  "Strict signatures: missing prototype for subroutine at " + Integer.toString(this.nodedata.getPos(sub)) + " (continuing)"
               );
            }
            int pos = this.nodedata.getPos(sub);
            int inferredParams = callsiteParams.getOrDefault(pos, 0);
            int movespParams = this.estimateParamsFromMovesp(sub);
            inferredParams = Math.max(inferredParams, movespParams);
            if (inferredParams < 0) {
               inferredParams = 0;
            }
            state.startPrototyping();
            state.setParamCount(inferredParams);
            // Default to void return when unknown; avoid inventing non-void types that can
            // distort call sites (e.g., dropping AssignCommand bodies).
            if (!state.type().isTyped() || state.type().byteValue() == Type.VT_NONE) {
               state.setReturnType(new Type(Type.VT_NONE), 0);
            }
            state.ensureParamPlaceholders();
            state.stopPrototyping(true);
         }
      }
   }

   private int estimateParamsFromMovesp(com.kotor.resource.formats.ncs.node.ASubroutine sub) {
      final int[] maxParams = new int[]{0};
      sub.apply(
         new PrunedDepthFirstAdapter() {
            @Override
            public void outAMoveSpCommand(AMoveSpCommand node) {
               int params = NodeUtils.stackOffsetToPos(node.getOffset());
               if (params > maxParams[0]) {
                  maxParams[0] = params;
               }
            }
         }
      );
      return maxParams[0];
   }
}

