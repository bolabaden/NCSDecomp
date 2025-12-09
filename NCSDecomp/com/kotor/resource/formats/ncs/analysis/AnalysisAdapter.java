// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.analysis;

import com.kotor.resource.formats.ncs.node.AActionCmd;
import com.kotor.resource.formats.ncs.node.AActionCommand;
import com.kotor.resource.formats.ncs.node.AActionJumpCmd;
import com.kotor.resource.formats.ncs.node.AAddBinaryOp;
import com.kotor.resource.formats.ncs.node.AAddVarCmd;
import com.kotor.resource.formats.ncs.node.AAndLogiiOp;
import com.kotor.resource.formats.ncs.node.ABinaryCmd;
import com.kotor.resource.formats.ncs.node.ABinaryCommand;
import com.kotor.resource.formats.ncs.node.ABitAndLogiiOp;
import com.kotor.resource.formats.ncs.node.ABpCmd;
import com.kotor.resource.formats.ncs.node.ABpCommand;
import com.kotor.resource.formats.ncs.node.ACommandBlock;
import com.kotor.resource.formats.ncs.node.ACompUnaryOp;
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
import com.kotor.resource.formats.ncs.node.ADecibpStackOp;
import com.kotor.resource.formats.ncs.node.ADecispStackOp;
import com.kotor.resource.formats.ncs.node.ADestructCmd;
import com.kotor.resource.formats.ncs.node.ADestructCommand;
import com.kotor.resource.formats.ncs.node.ADivBinaryOp;
import com.kotor.resource.formats.ncs.node.AEqualBinaryOp;
import com.kotor.resource.formats.ncs.node.AExclOrLogiiOp;
import com.kotor.resource.formats.ncs.node.AFloatConstant;
import com.kotor.resource.formats.ncs.node.AGeqBinaryOp;
import com.kotor.resource.formats.ncs.node.AGtBinaryOp;
import com.kotor.resource.formats.ncs.node.AIncibpStackOp;
import com.kotor.resource.formats.ncs.node.AIncispStackOp;
import com.kotor.resource.formats.ncs.node.AInclOrLogiiOp;
import com.kotor.resource.formats.ncs.node.AIntConstant;
import com.kotor.resource.formats.ncs.node.AJumpCmd;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpSubCmd;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.ALeqBinaryOp;
import com.kotor.resource.formats.ncs.node.ALogiiCmd;
import com.kotor.resource.formats.ncs.node.ALogiiCommand;
import com.kotor.resource.formats.ncs.node.ALtBinaryOp;
import com.kotor.resource.formats.ncs.node.AModBinaryOp;
import com.kotor.resource.formats.ncs.node.AMoveSpCommand;
import com.kotor.resource.formats.ncs.node.AMovespCmd;
import com.kotor.resource.formats.ncs.node.AMulBinaryOp;
import com.kotor.resource.formats.ncs.node.ANegUnaryOp;
import com.kotor.resource.formats.ncs.node.ANequalBinaryOp;
import com.kotor.resource.formats.ncs.node.ANonzeroJumpIf;
import com.kotor.resource.formats.ncs.node.ANotUnaryOp;
import com.kotor.resource.formats.ncs.node.AOrLogiiOp;
import com.kotor.resource.formats.ncs.node.AProgram;
import com.kotor.resource.formats.ncs.node.ARestorebpBpOp;
import com.kotor.resource.formats.ncs.node.AReturn;
import com.kotor.resource.formats.ncs.node.AReturnCmd;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.ASavebpBpOp;
import com.kotor.resource.formats.ncs.node.AShleftBinaryOp;
import com.kotor.resource.formats.ncs.node.AShrightBinaryOp;
import com.kotor.resource.formats.ncs.node.ASize;
import com.kotor.resource.formats.ncs.node.AStackCommand;
import com.kotor.resource.formats.ncs.node.AStackOpCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.AStringConstant;
import com.kotor.resource.formats.ncs.node.ASubBinaryOp;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.AUnaryCmd;
import com.kotor.resource.formats.ncs.node.AUnaryCommand;
import com.kotor.resource.formats.ncs.node.AUnrightBinaryOp;
import com.kotor.resource.formats.ncs.node.AZeroJumpIf;
import com.kotor.resource.formats.ncs.node.EOF;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.node.Start;
import com.kotor.resource.formats.ncs.node.TAction;
import com.kotor.resource.formats.ncs.node.TAdd;
import com.kotor.resource.formats.ncs.node.TBlank;
import com.kotor.resource.formats.ncs.node.TBoolandii;
import com.kotor.resource.formats.ncs.node.TComp;
import com.kotor.resource.formats.ncs.node.TConst;
import com.kotor.resource.formats.ncs.node.TCpdownbp;
import com.kotor.resource.formats.ncs.node.TCpdownsp;
import com.kotor.resource.formats.ncs.node.TCptopbp;
import com.kotor.resource.formats.ncs.node.TCptopsp;
import com.kotor.resource.formats.ncs.node.TDecibp;
import com.kotor.resource.formats.ncs.node.TDecisp;
import com.kotor.resource.formats.ncs.node.TDestruct;
import com.kotor.resource.formats.ncs.node.TDiv;
import com.kotor.resource.formats.ncs.node.TDot;
import com.kotor.resource.formats.ncs.node.TEqual;
import com.kotor.resource.formats.ncs.node.TExcorii;
import com.kotor.resource.formats.ncs.node.TFloatConstant;
import com.kotor.resource.formats.ncs.node.TGeq;
import com.kotor.resource.formats.ncs.node.TGt;
import com.kotor.resource.formats.ncs.node.TIncibp;
import com.kotor.resource.formats.ncs.node.TIncisp;
import com.kotor.resource.formats.ncs.node.TIncorii;
import com.kotor.resource.formats.ncs.node.TIntegerConstant;
import com.kotor.resource.formats.ncs.node.TJmp;
import com.kotor.resource.formats.ncs.node.TJnz;
import com.kotor.resource.formats.ncs.node.TJsr;
import com.kotor.resource.formats.ncs.node.TJz;
import com.kotor.resource.formats.ncs.node.TLPar;
import com.kotor.resource.formats.ncs.node.TLeq;
import com.kotor.resource.formats.ncs.node.TLogandii;
import com.kotor.resource.formats.ncs.node.TLogorii;
import com.kotor.resource.formats.ncs.node.TLt;
import com.kotor.resource.formats.ncs.node.TMod;
import com.kotor.resource.formats.ncs.node.TMovsp;
import com.kotor.resource.formats.ncs.node.TMul;
import com.kotor.resource.formats.ncs.node.TNeg;
import com.kotor.resource.formats.ncs.node.TNequal;
import com.kotor.resource.formats.ncs.node.TNop;
import com.kotor.resource.formats.ncs.node.TNot;
import com.kotor.resource.formats.ncs.node.TRPar;
import com.kotor.resource.formats.ncs.node.TRestorebp;
import com.kotor.resource.formats.ncs.node.TRetn;
import com.kotor.resource.formats.ncs.node.TRsadd;
import com.kotor.resource.formats.ncs.node.TSavebp;
import com.kotor.resource.formats.ncs.node.TSemi;
import com.kotor.resource.formats.ncs.node.TShleft;
import com.kotor.resource.formats.ncs.node.TShright;
import com.kotor.resource.formats.ncs.node.TStorestate;
import com.kotor.resource.formats.ncs.node.TStringLiteral;
import com.kotor.resource.formats.ncs.node.TSub;
import com.kotor.resource.formats.ncs.node.TT;
import com.kotor.resource.formats.ncs.node.TUnright;
import java.util.Hashtable;

/**
 * Base visitor that provides empty implementations for all grammar callbacks.
 * <p>
 * Subclasses override only the nodes they care about while still being able to
 * store per-node state via {@code in}/{@code out} maps.
 */
public class AnalysisAdapter implements Analysis {
   private Hashtable<Node, Object> in;
   private Hashtable<Node, Object> out;

   @Override
   public Object getIn(Node node) {
      return this.in == null ? null : this.in.get(node);
   }

   @Override
   public void setIn(Node node, Object in) {
      if (this.in == null) {
         this.in = new Hashtable<>(1);
      }

      if (in != null) {
         this.in.put(node, in);
      } else {
         this.in.remove(node);
      }
   }

   @Override
   public Object getOut(Node node) {
      return this.out == null ? null : this.out.get(node);
   }

   @Override
   public void setOut(Node node, Object out) {
      if (this.out == null) {
         this.out = new Hashtable<>(1);
      }

      if (out != null) {
         this.out.put(node, out);
      } else {
         this.out.remove(node);
      }
   }

   @Override
   public void caseStart(Start node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAProgram(AProgram node) {
      this.defaultCase(node);
   }

   @Override
   public void caseASubroutine(ASubroutine node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACommandBlock(ACommandBlock node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAAddVarCmd(AAddVarCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAActionJumpCmd(AActionJumpCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAConstCmd(AConstCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopydownspCmd(ACopydownspCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopytopspCmd(ACopytopspCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopydownbpCmd(ACopydownbpCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopytopbpCmd(ACopytopbpCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACondJumpCmd(ACondJumpCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAJumpCmd(AJumpCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAJumpSubCmd(AJumpSubCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAMovespCmd(AMovespCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseALogiiCmd(ALogiiCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAUnaryCmd(AUnaryCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseABinaryCmd(ABinaryCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseADestructCmd(ADestructCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseABpCmd(ABpCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAActionCmd(AActionCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAStackOpCmd(AStackOpCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAReturnCmd(AReturnCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAStoreStateCmd(AStoreStateCmd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAAndLogiiOp(AAndLogiiOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAOrLogiiOp(AOrLogiiOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAInclOrLogiiOp(AInclOrLogiiOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAExclOrLogiiOp(AExclOrLogiiOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseABitAndLogiiOp(ABitAndLogiiOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAEqualBinaryOp(AEqualBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseANequalBinaryOp(ANequalBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAGeqBinaryOp(AGeqBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAGtBinaryOp(AGtBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseALtBinaryOp(ALtBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseALeqBinaryOp(ALeqBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAShrightBinaryOp(AShrightBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAShleftBinaryOp(AShleftBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAUnrightBinaryOp(AUnrightBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAAddBinaryOp(AAddBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseASubBinaryOp(ASubBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAMulBinaryOp(AMulBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseADivBinaryOp(ADivBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAModBinaryOp(AModBinaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseANegUnaryOp(ANegUnaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACompUnaryOp(ACompUnaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseANotUnaryOp(ANotUnaryOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseADecispStackOp(ADecispStackOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAIncispStackOp(AIncispStackOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseADecibpStackOp(ADecibpStackOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAIncibpStackOp(AIncibpStackOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAIntConstant(AIntConstant node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAFloatConstant(AFloatConstant node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAStringConstant(AStringConstant node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAZeroJumpIf(AZeroJumpIf node) {
      this.defaultCase(node);
   }

   @Override
   public void caseANonzeroJumpIf(ANonzeroJumpIf node) {
      this.defaultCase(node);
   }

   @Override
   public void caseASavebpBpOp(ASavebpBpOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseARestorebpBpOp(ARestorebpBpOp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAConditionalJumpCommand(AConditionalJumpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAJumpCommand(AJumpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAJumpToSubroutine(AJumpToSubroutine node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAReturn(AReturn node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopyDownSpCommand(ACopyDownSpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopyTopSpCommand(ACopyTopSpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopyDownBpCommand(ACopyDownBpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseACopyTopBpCommand(ACopyTopBpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAMoveSpCommand(AMoveSpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseARsaddCommand(ARsaddCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAConstCommand(AConstCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAActionCommand(AActionCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseALogiiCommand(ALogiiCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseABinaryCommand(ABinaryCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAUnaryCommand(AUnaryCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAStackCommand(AStackCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseADestructCommand(ADestructCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseABpCommand(ABpCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseAStoreStateCommand(AStoreStateCommand node) {
      this.defaultCase(node);
   }

   @Override
   public void caseASize(ASize node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTLPar(TLPar node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTRPar(TRPar node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTSemi(TSemi node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTDot(TDot node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTCpdownsp(TCpdownsp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTRsadd(TRsadd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTCptopsp(TCptopsp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTConst(TConst node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTAction(TAction node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTLogandii(TLogandii node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTLogorii(TLogorii node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTIncorii(TIncorii node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTExcorii(TExcorii node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTBoolandii(TBoolandii node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTEqual(TEqual node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTNequal(TNequal node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTGeq(TGeq node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTGt(TGt node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTLt(TLt node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTLeq(TLeq node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTShleft(TShleft node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTShright(TShright node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTUnright(TUnright node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTAdd(TAdd node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTSub(TSub node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTMul(TMul node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTDiv(TDiv node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTMod(TMod node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTNeg(TNeg node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTComp(TComp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTMovsp(TMovsp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTJmp(TJmp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTJsr(TJsr node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTJz(TJz node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTRetn(TRetn node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTDestruct(TDestruct node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTNot(TNot node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTDecisp(TDecisp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTIncisp(TIncisp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTJnz(TJnz node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTCpdownbp(TCpdownbp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTCptopbp(TCptopbp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTDecibp(TDecibp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTIncibp(TIncibp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTSavebp(TSavebp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTRestorebp(TRestorebp node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTStorestate(TStorestate node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTNop(TNop node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTT(TT node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTStringLiteral(TStringLiteral node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTBlank(TBlank node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTIntegerConstant(TIntegerConstant node) {
      this.defaultCase(node);
   }

   @Override
   public void caseTFloatConstant(TFloatConstant node) {
      this.defaultCase(node);
   }

   @Override
   public void caseEOF(EOF node) {
      this.defaultCase(node);
   }

   public void defaultCase(Node node) {
   }
}

