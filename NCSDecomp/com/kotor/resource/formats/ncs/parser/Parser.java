// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.parser;

import com.kotor.resource.formats.ncs.analysis.Analysis;
import com.kotor.resource.formats.ncs.analysis.AnalysisAdapter;
import com.kotor.resource.formats.ncs.lexer.Lexer;
import com.kotor.resource.formats.ncs.lexer.LexerException;
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
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.ASavebpBpOp;
import com.kotor.resource.formats.ncs.node.AShleftBinaryOp;
import com.kotor.resource.formats.ncs.node.AShrightBinaryOp;
import com.kotor.resource.formats.ncs.node.ASize;
import com.kotor.resource.formats.ncs.node.AStackCommand;
import com.kotor.resource.formats.ncs.node.AStackOpCmd;
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
import com.kotor.resource.formats.ncs.node.PActionCommand;
import com.kotor.resource.formats.ncs.node.PBinaryCommand;
import com.kotor.resource.formats.ncs.node.PBinaryOp;
import com.kotor.resource.formats.ncs.node.PBpCommand;
import com.kotor.resource.formats.ncs.node.PBpOp;
import com.kotor.resource.formats.ncs.node.PCmd;
import com.kotor.resource.formats.ncs.node.PCommandBlock;
import com.kotor.resource.formats.ncs.node.PConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.PConstCommand;
import com.kotor.resource.formats.ncs.node.PConstant;
import com.kotor.resource.formats.ncs.node.PCopyDownBpCommand;
import com.kotor.resource.formats.ncs.node.PCopyDownSpCommand;
import com.kotor.resource.formats.ncs.node.PCopyTopBpCommand;
import com.kotor.resource.formats.ncs.node.PCopyTopSpCommand;
import com.kotor.resource.formats.ncs.node.PDestructCommand;
import com.kotor.resource.formats.ncs.node.PJumpCommand;
import com.kotor.resource.formats.ncs.node.PJumpIf;
import com.kotor.resource.formats.ncs.node.PJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.PLogiiCommand;
import com.kotor.resource.formats.ncs.node.PLogiiOp;
import com.kotor.resource.formats.ncs.node.PMoveSpCommand;
import com.kotor.resource.formats.ncs.node.PProgram;
import com.kotor.resource.formats.ncs.node.PReturn;
import com.kotor.resource.formats.ncs.node.PRsaddCommand;
import com.kotor.resource.formats.ncs.node.PSize;
import com.kotor.resource.formats.ncs.node.PStackCommand;
import com.kotor.resource.formats.ncs.node.PStackOp;
import com.kotor.resource.formats.ncs.node.PStoreStateCommand;
import com.kotor.resource.formats.ncs.node.PSubroutine;
import com.kotor.resource.formats.ncs.node.PUnaryCommand;
import com.kotor.resource.formats.ncs.node.PUnaryOp;
import com.kotor.resource.formats.ncs.node.Start;
import com.kotor.resource.formats.ncs.node.Switchable;
import com.kotor.resource.formats.ncs.node.TAction;
import com.kotor.resource.formats.ncs.node.TAdd;
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
import com.kotor.resource.formats.ncs.node.TLeq;
import com.kotor.resource.formats.ncs.node.TLogandii;
import com.kotor.resource.formats.ncs.node.TLogorii;
import com.kotor.resource.formats.ncs.node.TLt;
import com.kotor.resource.formats.ncs.node.TMod;
import com.kotor.resource.formats.ncs.node.TMovsp;
import com.kotor.resource.formats.ncs.node.TMul;
import com.kotor.resource.formats.ncs.node.TNeg;
import com.kotor.resource.formats.ncs.node.TNequal;
import com.kotor.resource.formats.ncs.node.TNot;
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
import com.kotor.resource.formats.ncs.node.Token;
import com.kotor.resource.formats.ncs.node.NoCast;
import com.kotor.resource.formats.ncs.node.TypedLinkedList;
import com.kotor.resource.formats.ncs.node.X1PCmd;
import com.kotor.resource.formats.ncs.node.X1PSubroutine;
import com.kotor.resource.formats.ncs.node.X2PCmd;
import com.kotor.resource.formats.ncs.node.X2PSubroutine;
import com.kotor.resource.formats.ncs.node.XPCmd;
import com.kotor.resource.formats.ncs.node.XPSubroutine;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * SableCC-generated parser that builds the AST from lexer tokens.
 * Note: factory methods named new0/new1/... are generated; do not rename.
 */
@SuppressWarnings({"unused"})
public class Parser {
   private static final int SHIFT = 0;
   private static final int REDUCE = 1;
   private static final int ACCEPT = 2;
   private static final int ERROR = 3;
   private static int[][][] actionTable;
   private static int[][][] gotoTable;
   private static String[] errorMessages;
   private static int[] errors;
   public final Analysis ignoredTokens = new AnalysisAdapter();
   protected Node node;
   private final Lexer lexer;
   private final ListIterator<State> stack = new LinkedList<State>().listIterator();
   private int last_shift;
   private int last_pos;
   private int last_line;
   private Token last_token;
   private final TokenIndex converter = new TokenIndex();
   private final int[] action = new int[2];

   public Parser(Lexer lexer) {
      this.lexer = lexer;
      if (actionTable == null) {
         try {
            DataInputStream s = new DataInputStream(new BufferedInputStream(Parser.class.getResourceAsStream("parser.dat")));
            int length = s.readInt();
            actionTable = new int[length][][];

            for (int i = 0; i < actionTable.length; i++) {
               length = s.readInt();
               actionTable[i] = new int[length][3];

               for (int j = 0; j < actionTable[i].length; j++) {
                  for (int k = 0; k < 3; k++) {
                     actionTable[i][j][k] = s.readInt();
                  }
               }
            }

            length = s.readInt();
            gotoTable = new int[length][][];

            for (int i = 0; i < gotoTable.length; i++) {
               length = s.readInt();
               gotoTable[i] = new int[length][2];

               for (int j = 0; j < gotoTable[i].length; j++) {
                  for (int k = 0; k < 2; k++) {
                     gotoTable[i][j][k] = s.readInt();
                  }
               }
            }

            length = s.readInt();
            errorMessages = new String[length];

            for (int i = 0; i < errorMessages.length; i++) {
               length = s.readInt();
               StringBuffer buffer = new StringBuffer();

               for (int j = 0; j < length; j++) {
                  buffer.append(s.readChar());
               }

               errorMessages[i] = buffer.toString();
            }

            length = s.readInt();
            errors = new int[length];

            for (int i = 0; i < errors.length; i++) {
               errors[i] = s.readInt();
            }

            s.close();
         } catch (Exception var7) {
            throw new RuntimeException("The file \"parser.dat\" is either missing or corrupted.");
         }
      }
   }

   protected void filter() throws ParserException, LexerException, IOException {
   }

   private int goTo(int index) {
      int state = this.state();
      int low = 1;
      int high = gotoTable[index].length - 1;
      int value = gotoTable[index][0][1];

      while (low <= high) {
         int middle = (low + high) / 2;
         if (state < gotoTable[index][middle][0]) {
            high = middle - 1;
         } else {
            if (state <= gotoTable[index][middle][0]) {
               value = gotoTable[index][middle][1];
               break;
            }

            low = middle + 1;
         }
      }

      return value;
   }

   private void push(int state, Node node, boolean filter) throws ParserException, LexerException, IOException {
      this.node = node;
      if (filter) {
         this.filter();
      }

      if (!this.stack.hasNext()) {
         this.stack.add(new State(state, this.node));
      } else {
         State s = this.stack.next();
         s.state = state;
         s.node = this.node;
      }
   }

   private int state() {
      State s = this.stack.previous();
      this.stack.next();
      return s.state;
   }

   private Node pop() {
      return this.stack.previous().node;
   }

   private int index(Switchable token) {
      this.converter.index = -1;
      token.apply(this.converter);
      return this.converter.index;
   }

   public Start parse() throws ParserException, LexerException, IOException {
      this.push(0, null, false);
      List<Token> ign = null;

      while (true) {
         for (; this.index(this.lexer.peek()) == -1; ign.add(this.lexer.next())) {
            if (ign == null) {
               ign = new TypedLinkedList<Token>(NoCast.<Token>instance());
            }
         }

         if (ign != null) {
            this.ignoredTokens.setIn(this.lexer.peek(), ign);
            ign = null;
         }

         this.last_pos = this.lexer.peek().getPos();
         this.last_line = this.lexer.peek().getLine();
         this.last_token = this.lexer.peek();
         int index = this.index(this.lexer.peek());
         this.action[0] = actionTable[this.state()][0][1];
         this.action[1] = actionTable[this.state()][0][2];
         int low = 1;
         int high = actionTable[this.state()].length - 1;

         while (low <= high) {
            int middle = (low + high) / 2;
            if (index < actionTable[this.state()][middle][0]) {
               high = middle - 1;
            } else {
               if (index <= actionTable[this.state()][middle][0]) {
                  this.action[0] = actionTable[this.state()][middle][1];
                  this.action[1] = actionTable[this.state()][middle][2];
                  break;
               }

               low = middle + 1;
            }
         }

         switch (this.action[0]) {
            case 0:
               this.push(this.action[1], this.lexer.next(), true);
               this.last_shift = this.action[1];
               break;
            case 1:
               switch (this.action[1]) {
                  case 0: {
                     Node node = this.new0();
                     this.push(this.goTo(0), node, true);
                     continue;
                  }
                  case 1: {
                     Node node = this.new1();
                     this.push(this.goTo(31), node, false);
                     continue;
                  }
                  case 2: {
                     Node node = this.new2();
                     this.push(this.goTo(31), node, false);
                     continue;
                  }
                  case 3: {
                     Node node = this.new3();
                     this.push(this.goTo(0), node, true);
                     continue;
                  }
                  case 4: {
                     Node node = this.new4();
                     this.push(this.goTo(1), node, true);
                     continue;
                  }
                  case 5: {
                     Node node = this.new5();
                     this.push(this.goTo(1), node, true);
                     continue;
                  }
                  case 6: {
                     Node node = this.new6();
                     this.push(this.goTo(2), node, true);
                     continue;
                  }
                  case 7: {
                     Node node = this.new7();
                     this.push(this.goTo(32), node, false);
                     continue;
                  }
                  case 8: {
                     Node node = this.new8();
                     this.push(this.goTo(32), node, false);
                     continue;
                  }
                  case 9: {
                     Node node = this.new9();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 10: {
                     Node node = this.new10();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 11: {
                     Node node = this.new11();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 12: {
                     Node node = this.new12();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 13: {
                     Node node = this.new13();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 14: {
                     Node node = this.new14();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 15: {
                     Node node = this.new15();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 16: {
                     Node node = this.new16();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 17: {
                     Node node = this.new17();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 18: {
                     Node node = this.new18();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 19: {
                     Node node = this.new19();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 20: {
                     Node node = this.new20();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 21: {
                     Node node = this.new21();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 22: {
                     Node node = this.new22();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 23: {
                     Node node = this.new23();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 24: {
                     Node node = this.new24();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 25: {
                     Node node = this.new25();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 26: {
                     Node node = this.new26();
                     this.push(this.goTo(3), node, true);
                     continue;
                  }
                  case 27: {
                     Node node = this.new27();
                     this.push(this.goTo(4), node, true);
                     continue;
                  }
                  case 28: {
                     Node node = this.new28();
                     this.push(this.goTo(4), node, true);
                     continue;
                  }
                  case 29: {
                     Node node = this.new29();
                     this.push(this.goTo(4), node, true);
                     continue;
                  }
                  case 30: {
                     Node node = this.new30();
                     this.push(this.goTo(4), node, true);
                     continue;
                  }
                  case 31: {
                     Node node = this.new31();
                     this.push(this.goTo(4), node, true);
                     continue;
                  }
                  case 32: {
                     Node node = this.new32();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 33: {
                     Node node = this.new33();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 34: {
                     Node node = this.new34();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 35: {
                     Node node = this.new35();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 36: {
                     Node node = this.new36();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 37: {
                     Node node = this.new37();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 38: {
                     Node node = this.new38();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 39: {
                     Node node = this.new39();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 40: {
                     Node node = this.new40();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 41: {
                     Node node = this.new41();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 42: {
                     Node node = this.new42();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 43: {
                     Node node = this.new43();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 44: {
                     Node node = this.new44();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 45: {
                     Node node = this.new45();
                     this.push(this.goTo(5), node, true);
                     continue;
                  }
                  case 46: {
                     Node node = this.new46();
                     this.push(this.goTo(6), node, true);
                     continue;
                  }
                  case 47: {
                     Node node = this.new47();
                     this.push(this.goTo(6), node, true);
                     continue;
                  }
                  case 48: {
                     Node node = this.new48();
                     this.push(this.goTo(6), node, true);
                     continue;
                  }
                  case 49: {
                     Node node = this.new49();
                     this.push(this.goTo(7), node, true);
                     continue;
                  }
                  case 50: {
                     Node node = this.new50();
                     this.push(this.goTo(7), node, true);
                     continue;
                  }
                  case 51: {
                     Node node = this.new51();
                     this.push(this.goTo(7), node, true);
                     continue;
                  }
                  case 52: {
                     Node node = this.new52();
                     this.push(this.goTo(7), node, true);
                     continue;
                  }
                  case 53: {
                     Node node = this.new53();
                     this.push(this.goTo(8), node, true);
                     continue;
                  }
                  case 54: {
                     Node node = this.new54();
                     this.push(this.goTo(8), node, true);
                     continue;
                  }
                  case 55: {
                     Node node = this.new55();
                     this.push(this.goTo(8), node, true);
                     continue;
                  }
                  case 56: {
                     Node node = this.new56();
                     this.push(this.goTo(9), node, true);
                     continue;
                  }
                  case 57: {
                     Node node = this.new57();
                     this.push(this.goTo(9), node, true);
                     continue;
                  }
                  case 58: {
                     Node node = this.new58();
                     this.push(this.goTo(10), node, true);
                     continue;
                  }
                  case 59: {
                     Node node = this.new59();
                     this.push(this.goTo(10), node, true);
                     continue;
                  }
                  case 60: {
                     Node node = this.new60();
                     this.push(this.goTo(11), node, true);
                     continue;
                  }
                  case 61: {
                     Node node = this.new61();
                     this.push(this.goTo(12), node, true);
                     continue;
                  }
                  case 62: {
                     Node node = this.new62();
                     this.push(this.goTo(13), node, true);
                     continue;
                  }
                  case 63: {
                     Node node = this.new63();
                     this.push(this.goTo(14), node, true);
                     continue;
                  }
                  case 64: {
                     Node node = this.new64();
                     this.push(this.goTo(15), node, true);
                     continue;
                  }
                  case 65: {
                     Node node = this.new65();
                     this.push(this.goTo(16), node, true);
                     continue;
                  }
                  case 66: {
                     Node node = this.new66();
                     this.push(this.goTo(17), node, true);
                     continue;
                  }
                  case 67: {
                     Node node = this.new67();
                     this.push(this.goTo(18), node, true);
                     continue;
                  }
                  case 68: {
                     Node node = this.new68();
                     this.push(this.goTo(19), node, true);
                     continue;
                  }
                  case 69: {
                     Node node = this.new69();
                     this.push(this.goTo(20), node, true);
                     continue;
                  }
                  case 70: {
                     Node node = this.new70();
                     this.push(this.goTo(21), node, true);
                     continue;
                  }
                  case 71: {
                     Node node = this.new71();
                     this.push(this.goTo(22), node, true);
                     continue;
                  }
                  case 72: {
                     Node node = this.new72();
                     this.push(this.goTo(23), node, true);
                     continue;
                  }
                  case 73: {
                     Node node = this.new73();
                     this.push(this.goTo(24), node, true);
                     continue;
                  }
                  case 74: {
                     Node node = this.new74();
                     this.push(this.goTo(24), node, true);
                     continue;
                  }
                  case 75: {
                     Node node = this.new75();
                     this.push(this.goTo(25), node, true);
                     continue;
                  }
                  case 76: {
                     Node node = this.new76();
                     this.push(this.goTo(26), node, true);
                     continue;
                  }
                  case 77: {
                     Node node = this.new77();
                     this.push(this.goTo(27), node, true);
                     continue;
                  }
                  case 78: {
                     Node node = this.new78();
                     this.push(this.goTo(28), node, true);
                     continue;
                  }
                  case 79: {
                     Node node = this.new79();
                     this.push(this.goTo(29), node, true);
                     continue;
                  }
                  case 80: {
                     Node node = this.new80();
                     this.push(this.goTo(30), node, true);
                     continue;
                  }
                  default:
                     continue;
               }
            case 2:
               EOF node2 = (EOF)this.lexer.next();
               PProgram node1 = (PProgram)this.pop();
               return new Start(node1, node2);
            case 3:
               throw new ParserException(this.last_token, "[" + this.last_line + "," + this.last_pos + "] " + errorMessages[errors[this.action[1]]]);
         }
      }
   }

   Node new0() {
      XPSubroutine node5 = (XPSubroutine)this.pop();
      PReturn node4 = (PReturn)this.pop();
      PJumpToSubroutine node3 = (PJumpToSubroutine)this.pop();
      PRsaddCommand node2 = null;
      PSize node1 = (PSize)this.pop();
      return new AProgram(node1, node2, node3, node4, node5);
   }

   Node new1() {
      PSubroutine node2 = (PSubroutine)this.pop();
      XPSubroutine node1 = (XPSubroutine)this.pop();
      return new X1PSubroutine(node1, node2);
   }

   Node new2() {
      PSubroutine node1 = (PSubroutine)this.pop();
      return new X2PSubroutine(node1);
   }

   Node new3() {
      XPSubroutine node5 = (XPSubroutine)this.pop();
      PReturn node4 = (PReturn)this.pop();
      PJumpToSubroutine node3 = (PJumpToSubroutine)this.pop();
      PRsaddCommand node2 = (PRsaddCommand)this.pop();
      PSize node1 = (PSize)this.pop();
      return new AProgram(node1, node2, node3, node4, node5);
   }

   Node new4() {
      PReturn node2 = (PReturn)this.pop();
      PCommandBlock node1 = null;
      return new ASubroutine(node1, node2);
   }

   Node new5() {
      PReturn node2 = (PReturn)this.pop();
      PCommandBlock node1 = (PCommandBlock)this.pop();
      return new ASubroutine(node1, node2);
   }

   Node new6() {
      XPCmd node1 = (XPCmd)this.pop();
      return new ACommandBlock(node1);
   }

   Node new7() {
      PCmd node2 = (PCmd)this.pop();
      XPCmd node1 = (XPCmd)this.pop();
      return new X1PCmd(node1, node2);
   }

   Node new8() {
      PCmd node1 = (PCmd)this.pop();
      return new X2PCmd(node1);
   }

   Node new9() {
      PRsaddCommand node1 = (PRsaddCommand)this.pop();
      return new AAddVarCmd(node1);
   }

   Node new10() {
      PReturn node4 = (PReturn)this.pop();
      PCommandBlock node3 = (PCommandBlock)this.pop();
      PJumpCommand node2 = (PJumpCommand)this.pop();
      PStoreStateCommand node1 = (PStoreStateCommand)this.pop();
      return new AActionJumpCmd(node1, node2, node3, node4);
   }

   Node new11() {
      PConstCommand node1 = (PConstCommand)this.pop();
      return new AConstCmd(node1);
   }

   Node new12() {
      PCopyDownSpCommand node1 = (PCopyDownSpCommand)this.pop();
      return new ACopydownspCmd(node1);
   }

   Node new13() {
      PCopyTopSpCommand node1 = (PCopyTopSpCommand)this.pop();
      return new ACopytopspCmd(node1);
   }

   Node new14() {
      PCopyDownBpCommand node1 = (PCopyDownBpCommand)this.pop();
      return new ACopydownbpCmd(node1);
   }

   Node new15() {
      PCopyTopBpCommand node1 = (PCopyTopBpCommand)this.pop();
      return new ACopytopbpCmd(node1);
   }

   Node new16() {
      PConditionalJumpCommand node1 = (PConditionalJumpCommand)this.pop();
      return new ACondJumpCmd(node1);
   }

   Node new17() {
      PJumpCommand node1 = (PJumpCommand)this.pop();
      return new AJumpCmd(node1);
   }

   Node new18() {
      PJumpToSubroutine node1 = (PJumpToSubroutine)this.pop();
      return new AJumpSubCmd(node1);
   }

   Node new19() {
      PMoveSpCommand node1 = (PMoveSpCommand)this.pop();
      return new AMovespCmd(node1);
   }

   Node new20() {
      PLogiiCommand node1 = (PLogiiCommand)this.pop();
      return new ALogiiCmd(node1);
   }

   Node new21() {
      PUnaryCommand node1 = (PUnaryCommand)this.pop();
      return new AUnaryCmd(node1);
   }

   Node new22() {
      PBinaryCommand node1 = (PBinaryCommand)this.pop();
      return new ABinaryCmd(node1);
   }

   Node new23() {
      PDestructCommand node1 = (PDestructCommand)this.pop();
      return new ADestructCmd(node1);
   }

   Node new24() {
      PBpCommand node1 = (PBpCommand)this.pop();
      return new ABpCmd(node1);
   }

   Node new25() {
      PActionCommand node1 = (PActionCommand)this.pop();
      return new AActionCmd(node1);
   }

   Node new26() {
      PStackCommand node1 = (PStackCommand)this.pop();
      return new AStackOpCmd(node1);
   }

   Node new27() {
      TLogandii node1 = (TLogandii)this.pop();
      return new AAndLogiiOp(node1);
   }

   Node new28() {
      TLogorii node1 = (TLogorii)this.pop();
      return new AOrLogiiOp(node1);
   }

   Node new29() {
      TIncorii node1 = (TIncorii)this.pop();
      return new AInclOrLogiiOp(node1);
   }

   Node new30() {
      TExcorii node1 = (TExcorii)this.pop();
      return new AExclOrLogiiOp(node1);
   }

   Node new31() {
      TBoolandii node1 = (TBoolandii)this.pop();
      return new ABitAndLogiiOp(node1);
   }

   Node new32() {
      TEqual node1 = (TEqual)this.pop();
      return new AEqualBinaryOp(node1);
   }

   Node new33() {
      TNequal node1 = (TNequal)this.pop();
      return new ANequalBinaryOp(node1);
   }

   Node new34() {
      TGeq node1 = (TGeq)this.pop();
      return new AGeqBinaryOp(node1);
   }

   Node new35() {
      TGt node1 = (TGt)this.pop();
      return new AGtBinaryOp(node1);
   }

   Node new36() {
      TLt node1 = (TLt)this.pop();
      return new ALtBinaryOp(node1);
   }

   Node new37() {
      TLeq node1 = (TLeq)this.pop();
      return new ALeqBinaryOp(node1);
   }

   Node new38() {
      TShright node1 = (TShright)this.pop();
      return new AShrightBinaryOp(node1);
   }

   Node new39() {
      TShleft node1 = (TShleft)this.pop();
      return new AShleftBinaryOp(node1);
   }

   Node new40() {
      TUnright node1 = (TUnright)this.pop();
      return new AUnrightBinaryOp(node1);
   }

   Node new41() {
      TAdd node1 = (TAdd)this.pop();
      return new AAddBinaryOp(node1);
   }

   Node new42() {
      TSub node1 = (TSub)this.pop();
      return new ASubBinaryOp(node1);
   }

   Node new43() {
      TMul node1 = (TMul)this.pop();
      return new AMulBinaryOp(node1);
   }

   Node new44() {
      TDiv node1 = (TDiv)this.pop();
      return new ADivBinaryOp(node1);
   }

   Node new45() {
      TMod node1 = (TMod)this.pop();
      return new AModBinaryOp(node1);
   }

   Node new46() {
      TNeg node1 = (TNeg)this.pop();
      return new ANegUnaryOp(node1);
   }

   Node new47() {
      TComp node1 = (TComp)this.pop();
      return new ACompUnaryOp(node1);
   }

   Node new48() {
      TNot node1 = (TNot)this.pop();
      return new ANotUnaryOp(node1);
   }

   Node new49() {
      TDecisp node1 = (TDecisp)this.pop();
      return new ADecispStackOp(node1);
   }

   Node new50() {
      TIncisp node1 = (TIncisp)this.pop();
      return new AIncispStackOp(node1);
   }

   Node new51() {
      TDecibp node1 = (TDecibp)this.pop();
      return new ADecibpStackOp(node1);
   }

   Node new52() {
      TIncibp node1 = (TIncibp)this.pop();
      return new AIncibpStackOp(node1);
   }

   Node new53() {
      TIntegerConstant node1 = (TIntegerConstant)this.pop();
      return new AIntConstant(node1);
   }

   Node new54() {
      TFloatConstant node1 = (TFloatConstant)this.pop();
      return new AFloatConstant(node1);
   }

   Node new55() {
      TStringLiteral node1 = (TStringLiteral)this.pop();
      return new AStringConstant(node1);
   }

   Node new56() {
      TJz node1 = (TJz)this.pop();
      return new AZeroJumpIf(node1);
   }

   Node new57() {
      TJnz node1 = (TJnz)this.pop();
      return new ANonzeroJumpIf(node1);
   }

   Node new58() {
      TSavebp node1 = (TSavebp)this.pop();
      return new ASavebpBpOp(node1);
   }

   Node new59() {
      TRestorebp node1 = (TRestorebp)this.pop();
      return new ARestorebpBpOp(node1);
   }

   Node new60() {
      TSemi node5 = (TSemi)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      PJumpIf node1 = (PJumpIf)this.pop();
      return new AConditionalJumpCommand(node1, node2, node3, node4, node5);
   }

   Node new61() {
      TSemi node5 = (TSemi)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TJmp node1 = (TJmp)this.pop();
      return new AJumpCommand(node1, node2, node3, node4, node5);
   }

   Node new62() {
      TSemi node5 = (TSemi)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TJsr node1 = (TJsr)this.pop();
      return new AJumpToSubroutine(node1, node2, node3, node4, node5);
   }

   Node new63() {
      TSemi node4 = (TSemi)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TRetn node1 = (TRetn)this.pop();
      return new AReturn(node1, node2, node3, node4);
   }

   Node new64() {
      TSemi node6 = (TSemi)this.pop();
      TIntegerConstant node5 = (TIntegerConstant)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TCpdownsp node1 = (TCpdownsp)this.pop();
      return new ACopyDownSpCommand(node1, node2, node3, node4, node5, node6);
   }

   Node new65() {
      TSemi node6 = (TSemi)this.pop();
      TIntegerConstant node5 = (TIntegerConstant)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TCptopsp node1 = (TCptopsp)this.pop();
      return new ACopyTopSpCommand(node1, node2, node3, node4, node5, node6);
   }

   Node new66() {
      TSemi node6 = (TSemi)this.pop();
      TIntegerConstant node5 = (TIntegerConstant)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TCpdownbp node1 = (TCpdownbp)this.pop();
      return new ACopyDownBpCommand(node1, node2, node3, node4, node5, node6);
   }

   Node new67() {
      TSemi node6 = (TSemi)this.pop();
      TIntegerConstant node5 = (TIntegerConstant)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TCptopbp node1 = (TCptopbp)this.pop();
      return new ACopyTopBpCommand(node1, node2, node3, node4, node5, node6);
   }

   Node new68() {
      TSemi node5 = (TSemi)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TMovsp node1 = (TMovsp)this.pop();
      return new AMoveSpCommand(node1, node2, node3, node4, node5);
   }

   Node new69() {
      TSemi node4 = (TSemi)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TRsadd node1 = (TRsadd)this.pop();
      return new ARsaddCommand(node1, node2, node3, node4);
   }

   Node new70() {
      TSemi node5 = (TSemi)this.pop();
      PConstant node4 = (PConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TConst node1 = (TConst)this.pop();
      return new AConstCommand(node1, node2, node3, node4, node5);
   }

   Node new71() {
      TSemi node6 = (TSemi)this.pop();
      TIntegerConstant node5 = (TIntegerConstant)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TAction node1 = (TAction)this.pop();
      return new AActionCommand(node1, node2, node3, node4, node5, node6);
   }

   Node new72() {
      TSemi node4 = (TSemi)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      PLogiiOp node1 = (PLogiiOp)this.pop();
      return new ALogiiCommand(node1, node2, node3, node4);
   }

   Node new73() {
      TSemi node5 = (TSemi)this.pop();
      TIntegerConstant node4 = null;
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      PBinaryOp node1 = (PBinaryOp)this.pop();
      return new ABinaryCommand(node1, node2, node3, node4, node5);
   }

   Node new74() {
      TSemi node5 = (TSemi)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      PBinaryOp node1 = (PBinaryOp)this.pop();
      return new ABinaryCommand(node1, node2, node3, node4, node5);
   }

   Node new75() {
      TSemi node4 = (TSemi)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      PUnaryOp node1 = (PUnaryOp)this.pop();
      return new AUnaryCommand(node1, node2, node3, node4);
   }

   Node new76() {
      TSemi node5 = (TSemi)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      PStackOp node1 = (PStackOp)this.pop();
      return new AStackCommand(node1, node2, node3, node4, node5);
   }

   Node new77() {
      TSemi node7 = (TSemi)this.pop();
      TIntegerConstant node6 = (TIntegerConstant)this.pop();
      TIntegerConstant node5 = (TIntegerConstant)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TDestruct node1 = (TDestruct)this.pop();
      return new ADestructCommand(node1, node2, node3, node4, node5, node6, node7);
   }

   Node new78() {
      TSemi node4 = (TSemi)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      PBpOp node1 = (PBpOp)this.pop();
      return new ABpCommand(node1, node2, node3, node4);
   }

   Node new79() {
      TSemi node6 = (TSemi)this.pop();
      TIntegerConstant node5 = (TIntegerConstant)this.pop();
      TIntegerConstant node4 = (TIntegerConstant)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TStorestate node1 = (TStorestate)this.pop();
      return new AStoreStateCommand(node1, node2, node3, node4, node5, node6);
   }

   Node new80() {
      TSemi node4 = (TSemi)this.pop();
      TIntegerConstant node3 = (TIntegerConstant)this.pop();
      TIntegerConstant node2 = (TIntegerConstant)this.pop();
      TT node1 = (TT)this.pop();
      return new ASize(node1, node2, node3, node4);
   }
}

