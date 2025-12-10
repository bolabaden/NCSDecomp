// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.analysis.PrunedDepthFirstAdapter;
import com.kotor.resource.formats.ncs.node.AActionCmd;
import com.kotor.resource.formats.ncs.node.AAddVarCmd;
import com.kotor.resource.formats.ncs.node.ABinaryCmd;
import com.kotor.resource.formats.ncs.node.ABpCmd;
import com.kotor.resource.formats.ncs.node.ACommandBlock;
import com.kotor.resource.formats.ncs.node.AConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.AConstCmd;
import com.kotor.resource.formats.ncs.node.ACopydownbpCmd;
import com.kotor.resource.formats.ncs.node.ACopydownspCmd;
import com.kotor.resource.formats.ncs.node.ACopytopbpCmd;
import com.kotor.resource.formats.ncs.node.ACopytopspCmd;
import com.kotor.resource.formats.ncs.node.ADestructCmd;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.ALogiiCmd;
import com.kotor.resource.formats.ncs.node.AMovespCmd;
import com.kotor.resource.formats.ncs.node.AStackOpCmd;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.AUnaryCmd;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.node.PCmd;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * Traces control-flow paths inside a subroutine to decide which branches are reachable.
 * Records jump decisions and enforces retry limits to avoid infinite exploration.
 */
public class SubroutinePathFinder extends PrunedDepthFirstAdapter {
   private NodeAnalysisData nodedata;
   private SubroutineAnalysisData subdata;
   private SubroutineState state;
   private boolean pathfailed;
   private boolean forcejump;
   private Hashtable<Integer, Integer> destinationcommands;
   private boolean limitretries;
   private int maxretry;
   private int retry;

   public SubroutinePathFinder(SubroutineState state, NodeAnalysisData nodedata, SubroutineAnalysisData subdata, int pass) {
      this.nodedata = nodedata;
      this.subdata = subdata;
      this.state = state;
      this.pathfailed = false;
      this.forcejump = false;
      this.limitretries = pass < 3;
      switch (pass) {
         case 0:
            this.maxretry = 10;
            break;
         case 1:
            this.maxretry = 15;
            break;
         case 2:
            this.maxretry = 25;
            break;
         default:
            this.maxretry = 9999;
            break;
      }
      this.retry = 0;
   }

   public void done() {
      this.nodedata = null;
      this.subdata = null;
      this.state = null;
      this.destinationcommands = null;
   }

   @Override
   public void inASubroutine(ASubroutine node) {
      this.state.startPrototyping();
   }

   @Override
   public void caseACommandBlock(ACommandBlock node) {
      this.inACommandBlock(node);
      LinkedList<PCmd> commands = node.getCmd();
      this.setupDestinationCommands(commands, node);
      int i = 0;

      while (i < commands.size()) {
         if (this.forcejump) {
            int nextPos = this.state.getCurrentDestination();
            i = this.destinationcommands.get(Integer.valueOf(nextPos));
            this.forcejump = false;
         } else if (this.pathfailed) {
            int nextPos = this.state.switchDecision();
            if (nextPos == -1 || this.limitretries && this.retry > this.maxretry) {
               this.state.stopPrototyping(false);
               return;
            }

            i = this.destinationcommands.get(Integer.valueOf(nextPos));
            this.pathfailed = false;
            this.retry++;
         }

         if (i < commands.size()) {
            commands.get(i).apply(this);
            i++;
         }
      }

      commands = null;
      this.outACommandBlock(node);
   }

   @Override
   public void outAConditionalJumpCommand(AConditionalJumpCommand node) {
      NodeUtils.getNextCommand(node, this.nodedata);
      if (!this.nodedata.logOrCode(node)) {
         this.state.addDecision(node, NodeUtils.getJumpDestinationPos(node));
      }
   }

   @Override
   public void outAJumpCommand(AJumpCommand node) {
      if (NodeUtils.getJumpDestinationPos(node) < this.nodedata.getPos(node)) {
         this.pathfailed = true;
      } else {
         this.state.addJump(node, NodeUtils.getJumpDestinationPos(node));
         this.forcejump = true;
      }
   }

   @Override
   public void outAJumpToSubroutine(AJumpToSubroutine node) {
      if (!this.subdata.isPrototyped(NodeUtils.getJumpDestinationPos(node), true)) {
         this.pathfailed = true;
      }
   }

   @Override
   public void caseAAddVarCmd(AAddVarCmd node) {
   }

   @Override
   public void caseAConstCmd(AConstCmd node) {
   }

   @Override
   public void caseACopydownspCmd(ACopydownspCmd node) {
   }

   @Override
   public void caseACopytopspCmd(ACopytopspCmd node) {
   }

   @Override
   public void caseACopydownbpCmd(ACopydownbpCmd node) {
   }

   @Override
   public void caseACopytopbpCmd(ACopytopbpCmd node) {
   }

   @Override
   public void caseAMovespCmd(AMovespCmd node) {
   }

   @Override
   public void caseALogiiCmd(ALogiiCmd node) {
   }

   @Override
   public void caseAUnaryCmd(AUnaryCmd node) {
   }

   @Override
   public void caseABinaryCmd(ABinaryCmd node) {
   }

   @Override
   public void caseADestructCmd(ADestructCmd node) {
   }

   @Override
   public void caseABpCmd(ABpCmd node) {
   }

   @Override
   public void caseAActionCmd(AActionCmd node) {
   }

   @Override
   public void caseAStackOpCmd(AStackOpCmd node) {
   }

   private void setupDestinationCommands(final LinkedList<PCmd> commands, Node ast) {
      this.destinationcommands = new Hashtable<Integer, Integer>(1);
      ast.apply(new PrunedDepthFirstAdapter() {
         @Override
         public void outAConditionalJumpCommand(AConditionalJumpCommand node) {
            int pos = NodeUtils.getJumpDestinationPos(node);
            SubroutinePathFinder.this.destinationcommands.put(pos, SubroutinePathFinder.this.getCommandIndexByPos(pos, commands));
         }

         @Override
         public void outAJumpCommand(AJumpCommand node) {
            int pos = NodeUtils.getJumpDestinationPos(node);
            SubroutinePathFinder.this.destinationcommands.put(pos, SubroutinePathFinder.this.getCommandIndexByPos(pos, commands));
         }

         @Override
         public void caseAAddVarCmd(AAddVarCmd node) {
         }

         @Override
         public void caseAConstCmd(AConstCmd node) {
         }

         @Override
         public void caseACopydownspCmd(ACopydownspCmd node) {
         }

         @Override
         public void caseACopytopspCmd(ACopytopspCmd node) {
         }

         @Override
         public void caseACopydownbpCmd(ACopydownbpCmd node) {
         }

         @Override
         public void caseACopytopbpCmd(ACopytopbpCmd node) {
         }

         @Override
         public void caseAMovespCmd(AMovespCmd node) {
         }

         @Override
         public void caseALogiiCmd(ALogiiCmd node) {
         }

         @Override
         public void caseAUnaryCmd(AUnaryCmd node) {
         }

         @Override
         public void caseABinaryCmd(ABinaryCmd node) {
         }

         @Override
         public void caseADestructCmd(ADestructCmd node) {
         }

         @Override
         public void caseABpCmd(ABpCmd node) {
         }

         @Override
         public void caseAActionCmd(AActionCmd node) {
         }

         @Override
         public void caseAStackOpCmd(AStackOpCmd node) {
         }
      });
   }

   private int getCommandIndexByPos(int pos, LinkedList<PCmd> commands) {
      Node node = (Node)commands.get(0);

      int i;
      for (i = 1; i < commands.size() && this.nodedata.getPos(node) < pos; i++) {
         node = (Node)commands.get(i);
         if (this.nodedata.getPos(node) == pos) {
            break;
         }
      }

      if (this.nodedata.getPos(node) > pos) {
         throw new RuntimeException("Unable to locate a command with position " + pos);
      } else {
         return i;
      }
   }
}

