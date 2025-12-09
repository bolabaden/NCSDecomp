// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.analysis.PrunedDepthFirstAdapter;
import com.kotor.resource.formats.ncs.node.AActionCmd;
import com.kotor.resource.formats.ncs.node.AActionJumpCmd;
import com.kotor.resource.formats.ncs.node.AAddVarCmd;
import com.kotor.resource.formats.ncs.node.ABinaryCmd;
import com.kotor.resource.formats.ncs.node.ABpCmd;
import com.kotor.resource.formats.ncs.node.ACommandBlock;
import com.kotor.resource.formats.ncs.node.AConstCmd;
import com.kotor.resource.formats.ncs.node.ACopydownbpCmd;
import com.kotor.resource.formats.ncs.node.ACopydownspCmd;
import com.kotor.resource.formats.ncs.node.ACopytopbpCmd;
import com.kotor.resource.formats.ncs.node.ACopytopspCmd;
import com.kotor.resource.formats.ncs.node.ADestructCmd;
import com.kotor.resource.formats.ncs.node.AJumpCmd;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.ALogiiCmd;
import com.kotor.resource.formats.ncs.node.AMovespCmd;
import com.kotor.resource.formats.ncs.node.AReturn;
import com.kotor.resource.formats.ncs.node.AReturnCmd;
import com.kotor.resource.formats.ncs.node.AStackOpCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.AUnaryCmd;
import com.kotor.resource.formats.ncs.node.PCmd;
import java.util.LinkedList;

/**
 * Rewrites action-jump constructs into linear command sequences in a subroutine.
 */
@SuppressWarnings({"unused"})
public class FlattenSub extends PrunedDepthFirstAdapter {
   private ASubroutine sub;
   private boolean actionjumpfound;
   private int i;
   private LinkedList<PCmd> commands;
   private NodeAnalysisData nodedata;

   public FlattenSub(ASubroutine sub, NodeAnalysisData nodedata) {
      this.setSub(sub);
      this.actionjumpfound = false;
      this.nodedata = nodedata;
   }

   public void done() {
      this.sub = null;
      this.commands = null;
      this.nodedata = null;
   }

   public void setSub(ASubroutine sub) {
      this.sub = sub;
   }

   @Override
   public void caseACommandBlock(ACommandBlock node) {
      this.commands = node.getCmd();
      this.i = 0;

      while (this.i < this.commands.size()) {
         this.commands.get(this.i).apply(this);
         if (this.actionjumpfound) {
            this.actionjumpfound = false;
         } else {
            this.i++;
         }
      }
   }

   @Override
   public void caseAActionJumpCmd(AActionJumpCmd node) {
      AStoreStateCommand sscommand = (AStoreStateCommand)node.getStoreStateCommand();
      AJumpCommand jmpcommand = (AJumpCommand)node.getJumpCommand();
      ACommandBlock cmdblock = (ACommandBlock)node.getCommandBlock();
      AReturn rtn = (AReturn)node.getReturn();
      AStoreStateCmd sscmd = new AStoreStateCmd(sscommand);
      AJumpCmd jmpcmd = new AJumpCmd(jmpcommand);
      AReturnCmd rtncmd = new AReturnCmd(rtn);
      this.nodedata.setPos(sscmd, this.nodedata.getPos(sscommand));
      this.nodedata.setPos(jmpcmd, this.nodedata.getPos(jmpcommand));
      this.nodedata.setPos(rtncmd, this.nodedata.getPos(rtn));
      int j = this.i;
      this.commands.set(j++, sscmd);
      this.commands.add(j++, jmpcmd);
      LinkedList<PCmd> subcmds = cmdblock.getCmd();

      while (subcmds.size() > 0) {
         this.commands.add(j++, subcmds.remove(0));
      }

      this.commands.add(j++, rtncmd);
      subcmds = null;
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
}

