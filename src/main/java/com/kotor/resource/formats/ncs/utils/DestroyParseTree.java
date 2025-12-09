// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.analysis.AnalysisAdapter;
import com.kotor.resource.formats.ncs.node.AActionCmd;
import com.kotor.resource.formats.ncs.node.AActionCommand;
import com.kotor.resource.formats.ncs.node.AActionJumpCmd;
import com.kotor.resource.formats.ncs.node.AAddVarCmd;
import com.kotor.resource.formats.ncs.node.ABinaryCmd;
import com.kotor.resource.formats.ncs.node.ABinaryCommand;
import com.kotor.resource.formats.ncs.node.ABpCmd;
import com.kotor.resource.formats.ncs.node.ABpCommand;
import com.kotor.resource.formats.ncs.node.ACommandBlock;
import com.kotor.resource.formats.ncs.node.ACondJumpCmd;
import com.kotor.resource.formats.ncs.node.AConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.AConstCmd;
import com.kotor.resource.formats.ncs.node.AConstCommand;
import com.kotor.resource.formats.ncs.node.ACopyDownBpCommand;
import com.kotor.resource.formats.ncs.node.ACopyDownSpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopBpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopSpCommand;
import com.kotor.resource.formats.ncs.node.ACopydownbpCmd;
import com.kotor.resource.formats.ncs.node.ACopydownspCmd;
import com.kotor.resource.formats.ncs.node.ACopytopbpCmd;
import com.kotor.resource.formats.ncs.node.ACopytopspCmd;
import com.kotor.resource.formats.ncs.node.ADestructCmd;
import com.kotor.resource.formats.ncs.node.ADestructCommand;
import com.kotor.resource.formats.ncs.node.AJumpCmd;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpSubCmd;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.ALogiiCmd;
import com.kotor.resource.formats.ncs.node.ALogiiCommand;
import com.kotor.resource.formats.ncs.node.AMoveSpCommand;
import com.kotor.resource.formats.ncs.node.AMovespCmd;
import com.kotor.resource.formats.ncs.node.AProgram;
import com.kotor.resource.formats.ncs.node.AReturn;
import com.kotor.resource.formats.ncs.node.AReturnCmd;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.AStackCommand;
import com.kotor.resource.formats.ncs.node.AStackOpCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.AUnaryCmd;
import com.kotor.resource.formats.ncs.node.AUnaryCommand;
import com.kotor.resource.formats.ncs.node.PCmd;
import com.kotor.resource.formats.ncs.node.PSubroutine;
import com.kotor.resource.formats.ncs.node.Start;
import java.util.Vector;

/**
 * Frees the parse tree by clearing child references post-processing to help GC.
 */
public class DestroyParseTree extends AnalysisAdapter {
   @Override
   public void caseStart(Start node) {
      node.getPProgram().apply(this);
      node.setPProgram(null);
   }

   @Override
   public void caseAProgram(AProgram node) {
      if (node.getSize() != null) {
         node.getSize().apply(this);
      }

      if (node.getConditional() != null) {
         node.getConditional().apply(this);
      }

      if (node.getJumpToSubroutine() != null) {
         node.getJumpToSubroutine().apply(this);
      }

      if (node.getReturn() != null) {
         node.getReturn().apply(this);
      }

      Object[] temp = node.getSubroutine().toArray();

      for (int i = 0; i < temp.length; i++) {
         ((PSubroutine)temp[i]).apply(this);
      }

      node.setSize(null);
      node.setConditional(null);
      node.setJumpToSubroutine(null);
      node.setReturn(null);
      node.setSubroutine(new Vector<PSubroutine>(1));
   }

   @Override
   public void caseASubroutine(ASubroutine node) {
      if (node.getCommandBlock() != null) {
         node.getCommandBlock().apply(this);
      }

      if (node.getReturn() != null) {
         node.getReturn().apply(this);
      }

      node.setCommandBlock(null);
      node.setReturn(null);
   }

   @Override
   public void caseACommandBlock(ACommandBlock node) {
      Object[] temp = node.getCmd().toArray();

      for (int i = 0; i < temp.length; i++) {
         ((PCmd)temp[i]).apply(this);
      }

      node.setCmd(new Vector<PCmd>(1));
   }

   @Override
   public void caseAAddVarCmd(AAddVarCmd node) {
      if (node.getRsaddCommand() != null) {
         node.getRsaddCommand().apply(this);
      }

      node.setRsaddCommand(null);
   }

   @Override
   public void caseAActionJumpCmd(AActionJumpCmd node) {
      if (node.getStoreStateCommand() != null) {
         node.getStoreStateCommand().apply(this);
      }

      if (node.getJumpCommand() != null) {
         node.getJumpCommand().apply(this);
      }

      if (node.getCommandBlock() != null) {
         node.getCommandBlock().apply(this);
      }

      if (node.getReturn() != null) {
         node.getReturn().apply(this);
      }

      node.setStoreStateCommand(null);
      node.setJumpCommand(null);
      node.setCommandBlock(null);
      node.setReturn(null);
   }

   @Override
   public void caseAConstCmd(AConstCmd node) {
      if (node.getConstCommand() != null) {
         node.getConstCommand().apply(this);
      }

      node.setConstCommand(null);
   }

   @Override
   public void caseACopydownspCmd(ACopydownspCmd node) {
      if (node.getCopyDownSpCommand() != null) {
         node.getCopyDownSpCommand().apply(this);
      }

      node.setCopyDownSpCommand(null);
   }

   @Override
   public void caseACopytopspCmd(ACopytopspCmd node) {
      if (node.getCopyTopSpCommand() != null) {
         node.getCopyTopSpCommand().apply(this);
      }

      node.setCopyTopSpCommand(null);
   }

   @Override
   public void caseACopydownbpCmd(ACopydownbpCmd node) {
      if (node.getCopyDownBpCommand() != null) {
         node.getCopyDownBpCommand().apply(this);
      }

      node.setCopyDownBpCommand(null);
   }

   @Override
   public void caseACopytopbpCmd(ACopytopbpCmd node) {
      if (node.getCopyTopBpCommand() != null) {
         node.getCopyTopBpCommand().apply(this);
      }

      node.setCopyTopBpCommand(null);
   }

   @Override
   public void caseACondJumpCmd(ACondJumpCmd node) {
      if (node.getConditionalJumpCommand() != null) {
         node.getConditionalJumpCommand().apply(this);
      }

      node.setConditionalJumpCommand(null);
   }

   @Override
   public void caseAJumpCmd(AJumpCmd node) {
      if (node.getJumpCommand() != null) {
         node.getJumpCommand().apply(this);
      }

      node.setJumpCommand(null);
   }

   @Override
   public void caseAJumpSubCmd(AJumpSubCmd node) {
      if (node.getJumpToSubroutine() != null) {
         node.getJumpToSubroutine().apply(this);
      }

      node.setJumpToSubroutine(null);
   }

   @Override
   public void caseAMovespCmd(AMovespCmd node) {
      if (node.getMoveSpCommand() != null) {
         node.getMoveSpCommand().apply(this);
      }

      node.setMoveSpCommand(null);
   }

   @Override
   public void caseALogiiCmd(ALogiiCmd node) {
      if (node.getLogiiCommand() != null) {
         node.getLogiiCommand().apply(this);
      }

      node.setLogiiCommand(null);
   }

   @Override
   public void caseAUnaryCmd(AUnaryCmd node) {
      if (node.getUnaryCommand() != null) {
         node.getUnaryCommand().apply(this);
      }

      node.setUnaryCommand(null);
   }

   @Override
   public void caseABinaryCmd(ABinaryCmd node) {
      if (node.getBinaryCommand() != null) {
         node.getBinaryCommand().apply(this);
      }

      node.setBinaryCommand(null);
   }

   @Override
   public void caseADestructCmd(ADestructCmd node) {
      if (node.getDestructCommand() != null) {
         node.getDestructCommand().apply(this);
      }

      node.setDestructCommand(null);
   }

   @Override
   public void caseABpCmd(ABpCmd node) {
      if (node.getBpCommand() != null) {
         node.getBpCommand().apply(this);
      }

      node.setBpCommand(null);
   }

   @Override
   public void caseAActionCmd(AActionCmd node) {
      if (node.getActionCommand() != null) {
         node.getActionCommand().apply(this);
      }

      node.setActionCommand(null);
   }

   @Override
   public void caseAStackOpCmd(AStackOpCmd node) {
      if (node.getStackCommand() != null) {
         node.getStackCommand().apply(this);
      }

      node.setStackCommand(null);
   }

   @Override
   public void caseAReturnCmd(AReturnCmd node) {
      if (node.getReturn() != null) {
         node.getReturn().apply(this);
      }

      node.setReturn(null);
   }

   @Override
   public void caseAStoreStateCmd(AStoreStateCmd node) {
      if (node.getStoreStateCommand() != null) {
         node.getStoreStateCommand().apply(this);
      }

      node.setStoreStateCommand(null);
   }

   @Override
   public void caseAConditionalJumpCommand(AConditionalJumpCommand node) {
      node.setJumpIf(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSemi(null);
   }

   @Override
   public void caseAJumpCommand(AJumpCommand node) {
      node.setJmp(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSemi(null);
   }

   @Override
   public void caseAJumpToSubroutine(AJumpToSubroutine node) {
      node.setJsr(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSemi(null);
   }

   @Override
   public void caseAReturn(AReturn node) {
      node.setRetn(null);
      node.setPos(null);
      node.setType(null);
      node.setSemi(null);
   }

   @Override
   public void caseACopyDownSpCommand(ACopyDownSpCommand node) {
      node.setCpdownsp(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSize(null);
      node.setSemi(null);
   }

   @Override
   public void caseACopyTopSpCommand(ACopyTopSpCommand node) {
      node.setCptopsp(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSize(null);
      node.setSemi(null);
   }

   @Override
   public void caseACopyDownBpCommand(ACopyDownBpCommand node) {
      node.setCpdownbp(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSize(null);
      node.setSemi(null);
   }

   @Override
   public void caseACopyTopBpCommand(ACopyTopBpCommand node) {
      node.setCptopbp(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSize(null);
      node.setSemi(null);
   }

   @Override
   public void caseAMoveSpCommand(AMoveSpCommand node) {
      node.setMovsp(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSemi(null);
   }

   @Override
   public void caseARsaddCommand(ARsaddCommand node) {
      node.setRsadd(null);
      node.setPos(null);
      node.setType(null);
      node.setSemi(null);
   }

   @Override
   public void caseAConstCommand(AConstCommand node) {
      node.setConst(null);
      node.setPos(null);
      node.setType(null);
      node.setConstant(null);
      node.setSemi(null);
   }

   @Override
   public void caseAActionCommand(AActionCommand node) {
      node.setAction(null);
      node.setPos(null);
      node.setType(null);
      node.setId(null);
      node.setArgCount(null);
      node.setSemi(null);
   }

   @Override
   public void caseALogiiCommand(ALogiiCommand node) {
      node.setLogiiOp(null);
      node.setPos(null);
      node.setType(null);
      node.setSemi(null);
   }

   @Override
   public void caseABinaryCommand(ABinaryCommand node) {
      node.setBinaryOp(null);
      node.setPos(null);
      node.setType(null);
      node.setSize(null);
      node.setSemi(null);
   }

   @Override
   public void caseAUnaryCommand(AUnaryCommand node) {
      node.setUnaryOp(null);
      node.setPos(null);
      node.setType(null);
      node.setSemi(null);
   }

   @Override
   public void caseAStackCommand(AStackCommand node) {
      node.setStackOp(null);
      node.setPos(null);
      node.setType(null);
      node.setOffset(null);
      node.setSemi(null);
   }

   @Override
   public void caseADestructCommand(ADestructCommand node) {
      node.setDestruct(null);
      node.setPos(null);
      node.setType(null);
      node.setSizeRem(null);
      node.setOffset(null);
      node.setSizeSave(null);
      node.setSemi(null);
   }

   @Override
   public void caseABpCommand(ABpCommand node) {
      node.setBpOp(null);
      node.setPos(null);
      node.setType(null);
      node.setSemi(null);
   }

   @Override
   public void caseAStoreStateCommand(AStoreStateCommand node) {
      node.setStorestate(null);
      node.setPos(null);
      node.setOffset(null);
      node.setSizeBp(null);
      node.setSizeSp(null);
      node.setSemi(null);
   }
}

