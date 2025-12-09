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
import com.kotor.resource.formats.ncs.node.Switch;
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

/**
 * Visitor contract produced by SableCC for walking the decompiler AST.
 * <p>
 * Implementations receive callbacks for every grammar production and can store
 * arbitrary analysis state via {@code in}/{@code out} maps keyed by nodes.
 */
public interface Analysis extends Switch {
   Object getIn(Node var1);

   void setIn(Node var1, Object var2);

   Object getOut(Node var1);

   void setOut(Node var1, Object var2);

   void caseStart(Start var1);

   void caseAProgram(AProgram var1);

   void caseASubroutine(ASubroutine var1);

   void caseACommandBlock(ACommandBlock var1);

   void caseAAddVarCmd(AAddVarCmd var1);

   void caseAActionJumpCmd(AActionJumpCmd var1);

   void caseAConstCmd(AConstCmd var1);

   void caseACopydownspCmd(ACopydownspCmd var1);

   void caseACopytopspCmd(ACopytopspCmd var1);

   void caseACopydownbpCmd(ACopydownbpCmd var1);

   void caseACopytopbpCmd(ACopytopbpCmd var1);

   void caseACondJumpCmd(ACondJumpCmd var1);

   void caseAJumpCmd(AJumpCmd var1);

   void caseAJumpSubCmd(AJumpSubCmd var1);

   void caseAMovespCmd(AMovespCmd var1);

   void caseALogiiCmd(ALogiiCmd var1);

   void caseAUnaryCmd(AUnaryCmd var1);

   void caseABinaryCmd(ABinaryCmd var1);

   void caseADestructCmd(ADestructCmd var1);

   void caseABpCmd(ABpCmd var1);

   void caseAActionCmd(AActionCmd var1);

   void caseAStackOpCmd(AStackOpCmd var1);

   void caseAReturnCmd(AReturnCmd var1);

   void caseAStoreStateCmd(AStoreStateCmd var1);

   void caseAAndLogiiOp(AAndLogiiOp var1);

   void caseAOrLogiiOp(AOrLogiiOp var1);

   void caseAInclOrLogiiOp(AInclOrLogiiOp var1);

   void caseAExclOrLogiiOp(AExclOrLogiiOp var1);

   void caseABitAndLogiiOp(ABitAndLogiiOp var1);

   void caseAEqualBinaryOp(AEqualBinaryOp var1);

   void caseANequalBinaryOp(ANequalBinaryOp var1);

   void caseAGeqBinaryOp(AGeqBinaryOp var1);

   void caseAGtBinaryOp(AGtBinaryOp var1);

   void caseALtBinaryOp(ALtBinaryOp var1);

   void caseALeqBinaryOp(ALeqBinaryOp var1);

   void caseAShrightBinaryOp(AShrightBinaryOp var1);

   void caseAShleftBinaryOp(AShleftBinaryOp var1);

   void caseAUnrightBinaryOp(AUnrightBinaryOp var1);

   void caseAAddBinaryOp(AAddBinaryOp var1);

   void caseASubBinaryOp(ASubBinaryOp var1);

   void caseAMulBinaryOp(AMulBinaryOp var1);

   void caseADivBinaryOp(ADivBinaryOp var1);

   void caseAModBinaryOp(AModBinaryOp var1);

   void caseANegUnaryOp(ANegUnaryOp var1);

   void caseACompUnaryOp(ACompUnaryOp var1);

   void caseANotUnaryOp(ANotUnaryOp var1);

   void caseADecispStackOp(ADecispStackOp var1);

   void caseAIncispStackOp(AIncispStackOp var1);

   void caseADecibpStackOp(ADecibpStackOp var1);

   void caseAIncibpStackOp(AIncibpStackOp var1);

   void caseAIntConstant(AIntConstant var1);

   void caseAFloatConstant(AFloatConstant var1);

   void caseAStringConstant(AStringConstant var1);

   void caseAZeroJumpIf(AZeroJumpIf var1);

   void caseANonzeroJumpIf(ANonzeroJumpIf var1);

   void caseASavebpBpOp(ASavebpBpOp var1);

   void caseARestorebpBpOp(ARestorebpBpOp var1);

   void caseAConditionalJumpCommand(AConditionalJumpCommand var1);

   void caseAJumpCommand(AJumpCommand var1);

   void caseAJumpToSubroutine(AJumpToSubroutine var1);

   void caseAReturn(AReturn var1);

   void caseACopyDownSpCommand(ACopyDownSpCommand var1);

   void caseACopyTopSpCommand(ACopyTopSpCommand var1);

   void caseACopyDownBpCommand(ACopyDownBpCommand var1);

   void caseACopyTopBpCommand(ACopyTopBpCommand var1);

   void caseAMoveSpCommand(AMoveSpCommand var1);

   void caseARsaddCommand(ARsaddCommand var1);

   void caseAConstCommand(AConstCommand var1);

   void caseAActionCommand(AActionCommand var1);

   void caseALogiiCommand(ALogiiCommand var1);

   void caseABinaryCommand(ABinaryCommand var1);

   void caseAUnaryCommand(AUnaryCommand var1);

   void caseAStackCommand(AStackCommand var1);

   void caseADestructCommand(ADestructCommand var1);

   void caseABpCommand(ABpCommand var1);

   void caseAStoreStateCommand(AStoreStateCommand var1);

   void caseASize(ASize var1);

   void caseTLPar(TLPar var1);

   void caseTRPar(TRPar var1);

   void caseTSemi(TSemi var1);

   void caseTDot(TDot var1);

   void caseTCpdownsp(TCpdownsp var1);

   void caseTRsadd(TRsadd var1);

   void caseTCptopsp(TCptopsp var1);

   void caseTConst(TConst var1);

   void caseTAction(TAction var1);

   void caseTLogandii(TLogandii var1);

   void caseTLogorii(TLogorii var1);

   void caseTIncorii(TIncorii var1);

   void caseTExcorii(TExcorii var1);

   void caseTBoolandii(TBoolandii var1);

   void caseTEqual(TEqual var1);

   void caseTNequal(TNequal var1);

   void caseTGeq(TGeq var1);

   void caseTGt(TGt var1);

   void caseTLt(TLt var1);

   void caseTLeq(TLeq var1);

   void caseTShleft(TShleft var1);

   void caseTShright(TShright var1);

   void caseTUnright(TUnright var1);

   void caseTAdd(TAdd var1);

   void caseTSub(TSub var1);

   void caseTMul(TMul var1);

   void caseTDiv(TDiv var1);

   void caseTMod(TMod var1);

   void caseTNeg(TNeg var1);

   void caseTComp(TComp var1);

   void caseTMovsp(TMovsp var1);

   void caseTJmp(TJmp var1);

   void caseTJsr(TJsr var1);

   void caseTJz(TJz var1);

   void caseTRetn(TRetn var1);

   void caseTDestruct(TDestruct var1);

   void caseTNot(TNot var1);

   void caseTDecisp(TDecisp var1);

   void caseTIncisp(TIncisp var1);

   void caseTJnz(TJnz var1);

   void caseTCpdownbp(TCpdownbp var1);

   void caseTCptopbp(TCptopbp var1);

   void caseTDecibp(TDecibp var1);

   void caseTIncibp(TIncibp var1);

   void caseTSavebp(TSavebp var1);

   void caseTRestorebp(TRestorebp var1);

   void caseTStorestate(TStorestate var1);

   void caseTNop(TNop var1);

   void caseTT(TT var1);

   void caseTStringLiteral(TStringLiteral var1);

   void caseTBlank(TBlank var1);

   void caseTIntegerConstant(TIntegerConstant var1);

   void caseTFloatConstant(TFloatConstant var1);

   void caseEOF(EOF var1);
}

