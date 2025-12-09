// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.lexer;

import com.kotor.resource.formats.ncs.node.EOF;
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
import com.kotor.resource.formats.ncs.node.Token;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PushbackReader;

/**
 * SableCC-generated lexer for NCS bytecode tokenization. Generated tables and
 * methods should remain unchanged; names follow generator conventions.
 */
public class Lexer {
   private static int[][][][] gotoTable;
   private static int[][] accept;
   protected Token token;
   protected Lexer.State state = Lexer.State.INITIAL;
   private PushbackReader in;
   private int line;
   private int pos;
   private boolean cr;
   private boolean eof;
   private final StringBuffer text = new StringBuffer();

   public Lexer(PushbackReader in) {
      this.in = in;
      if (gotoTable == null) {
         try {
            DataInputStream s = new DataInputStream(
                  new BufferedInputStream(Lexer.class.getResourceAsStream("lexer.dat")));
            int length = s.readInt();
            gotoTable = new int[length][][][];

            for (int i = 0; i < gotoTable.length; i++) {
               length = s.readInt();
               gotoTable[i] = new int[length][][];

               for (int j = 0; j < gotoTable[i].length; j++) {
                  length = s.readInt();
                  gotoTable[i][j] = new int[length][3];

                  for (int k = 0; k < gotoTable[i][j].length; k++) {
                     for (int l = 0; l < 3; l++) {
                        gotoTable[i][j][k][l] = s.readInt();
                     }
                  }
               }
            }

            length = s.readInt();
            accept = new int[length][];

            for (int i = 0; i < accept.length; i++) {
               length = s.readInt();
               accept[i] = new int[length];

               for (int j = 0; j < accept[i].length; j++) {
                  accept[i][j] = s.readInt();
               }
            }

            s.close();
         } catch (Exception var8) {
            throw new RuntimeException("The file \"lexer.dat\" is either missing or corrupted.");
         }
      }
   }

   protected void filter() throws LexerException, IOException {
   }

   public Token peek() throws LexerException, IOException {
      while (this.token == null) {
         this.token = this.getToken();
         this.filter();
      }

      return this.token;
   }

   public Token next() throws LexerException, IOException {
      while (this.token == null) {
         this.token = this.getToken();
         this.filter();
      }

      Token result = this.token;
      this.token = null;
      return result;
   }

   protected Token getToken() throws IOException, LexerException {
      int dfa_state = 0;
      int start_pos = this.pos;
      int start_line = this.line;
      int accept_state = -1;
      int accept_token = -1;
      int accept_length = -1;
      int accept_pos = -1;
      int accept_line = -1;
      int[][][] gotoTable = Lexer.gotoTable[this.state.id()];
      int[] accept = Lexer.accept[this.state.id()];
      this.text.setLength(0);

      while (true) {
         int c = this.getChar();
         if (c == -1) {
            dfa_state = -1;
         } else {
            switch (c) {
               case 10:
                  if (this.cr) {
                     this.cr = false;
                  } else {
                     this.line++;
                     this.pos = 0;
                  }
                  break;
               case 11:
               case 12:
               default:
                  this.pos++;
                  this.cr = false;
                  break;
               case 13:
                  this.line++;
                  this.pos = 0;
                  this.cr = true;
            }

            this.text.append((char) c);

            do {
               int oldState = dfa_state < -1 ? -2 - dfa_state : dfa_state;
               dfa_state = -1;
               int[][] tmp1 = gotoTable[oldState];
               int low = 0;
               int high = tmp1.length - 1;

               while (low <= high) {
                  int middle = (low + high) / 2;
                  int[] tmp2 = tmp1[middle];
                  if (c < tmp2[0]) {
                     high = middle - 1;
                  } else {
                     if (c <= tmp2[1]) {
                        dfa_state = tmp2[2];
                        break;
                     }

                     low = middle + 1;
                  }
               }
            } while (dfa_state < -1);
         }

         if (dfa_state >= 0) {
            if (accept[dfa_state] != -1) {
               accept_state = dfa_state;
               accept_token = accept[dfa_state];
               accept_length = this.text.length();
               accept_pos = this.pos;
               accept_line = this.line;
            }
         } else {
            if (accept_state == -1) {
               if (this.text.length() > 0) {
                  throw new LexerException(
                        "[" + (start_line + 1) + "," + (start_pos + 1) + "]" + " Unknown token: " + this.text);
               }

               return new EOF(start_line + 1, start_pos + 1);
            }

            switch (accept_token) {
               case 0: {
                  Token token = this.new0(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 1: {
                  Token token = this.new1(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 2: {
                  Token token = this.new2(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 3: {
                  Token token = this.new3(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 4: {
                  Token token = this.new4(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 5: {
                  Token token = this.new5(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 6: {
                  Token token = this.new6(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 7: {
                  Token token = this.new7(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 8: {
                  Token token = this.new8(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 9: {
                  Token token = this.new9(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 10: {
                  Token token = this.new10(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 11: {
                  Token token = this.new11(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 12: {
                  Token token = this.new12(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 13: {
                  Token token = this.new13(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 14: {
                  Token token = this.new14(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 15: {
                  Token token = this.new15(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 16: {
                  Token token = this.new16(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 17: {
                  Token token = this.new17(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 18: {
                  Token token = this.new18(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 19: {
                  Token token = this.new19(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 20: {
                  Token token = this.new20(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 21: {
                  Token token = this.new21(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 22: {
                  Token token = this.new22(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 23: {
                  Token token = this.new23(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 24: {
                  Token token = this.new24(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 25: {
                  Token token = this.new25(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 26: {
                  Token token = this.new26(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 27: {
                  Token token = this.new27(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 28: {
                  Token token = this.new28(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 29: {
                  Token token = this.new29(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 30: {
                  Token token = this.new30(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 31: {
                  Token token = this.new31(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 32: {
                  Token token = this.new32(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 33: {
                  Token token = this.new33(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 34: {
                  Token token = this.new34(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 35: {
                  Token token = this.new35(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 36: {
                  Token token = this.new36(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 37: {
                  Token token = this.new37(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 38: {
                  Token token = this.new38(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 39: {
                  Token token = this.new39(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 40: {
                  Token token = this.new40(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 41: {
                  Token token = this.new41(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 42: {
                  Token token = this.new42(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 43: {
                  Token token = this.new43(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 44: {
                  Token token = this.new44(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 45: {
                  Token token = this.new45(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 46: {
                  Token token = this.new46(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 47: {
                  Token token = this.new47(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 48: {
                  Token token = this.new48(start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 49: {
                  Token token = this.new49(this.getText(accept_length), start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 50: {
                  Token token = this.new50(this.getText(accept_length), start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 51: {
                  Token token = this.new51(this.getText(accept_length), start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
               case 52: {
                  Token token = this.new52(this.getText(accept_length), start_line + 1, start_pos + 1);
                  this.pushBack(accept_length);
                  this.pos = accept_pos;
                  this.line = accept_line;
                  return token;
               }
            }
         }
      }
   }

   Token new0(int line, int pos) {
      return new TLPar(line, pos);
   }

   Token new1(int line, int pos) {
      return new TRPar(line, pos);
   }

   Token new2(int line, int pos) {
      return new TSemi(line, pos);
   }

   Token new3(int line, int pos) {
      return new TDot(line, pos);
   }

   Token new4(int line, int pos) {
      return new TCpdownsp(line, pos);
   }

   Token new5(int line, int pos) {
      return new TRsadd(line, pos);
   }

   Token new6(int line, int pos) {
      return new TCptopsp(line, pos);
   }

   Token new7(int line, int pos) {
      return new TConst(line, pos);
   }

   Token new8(int line, int pos) {
      return new TAction(line, pos);
   }

   Token new9(int line, int pos) {
      return new TLogandii(line, pos);
   }

   Token new10(int line, int pos) {
      return new TLogorii(line, pos);
   }

   Token new11(int line, int pos) {
      return new TIncorii(line, pos);
   }

   Token new12(int line, int pos) {
      return new TExcorii(line, pos);
   }

   Token new13(int line, int pos) {
      return new TBoolandii(line, pos);
   }

   Token new14(int line, int pos) {
      return new TEqual(line, pos);
   }

   Token new15(int line, int pos) {
      return new TNequal(line, pos);
   }

   Token new16(int line, int pos) {
      return new TGeq(line, pos);
   }

   Token new17(int line, int pos) {
      return new TGt(line, pos);
   }

   Token new18(int line, int pos) {
      return new TLt(line, pos);
   }

   Token new19(int line, int pos) {
      return new TLeq(line, pos);
   }

   Token new20(int line, int pos) {
      return new TShleft(line, pos);
   }

   Token new21(int line, int pos) {
      return new TShright(line, pos);
   }

   Token new22(int line, int pos) {
      return new TUnright(line, pos);
   }

   Token new23(int line, int pos) {
      return new TAdd(line, pos);
   }

   Token new24(int line, int pos) {
      return new TSub(line, pos);
   }

   Token new25(int line, int pos) {
      return new TMul(line, pos);
   }

   Token new26(int line, int pos) {
      return new TDiv(line, pos);
   }

   Token new27(int line, int pos) {
      return new TMod(line, pos);
   }

   Token new28(int line, int pos) {
      return new TNeg(line, pos);
   }

   Token new29(int line, int pos) {
      return new TComp(line, pos);
   }

   Token new30(int line, int pos) {
      return new TMovsp(line, pos);
   }

   Token new31(int line, int pos) {
      return new TJmp(line, pos);
   }

   Token new32(int line, int pos) {
      return new TJsr(line, pos);
   }

   Token new33(int line, int pos) {
      return new TJz(line, pos);
   }

   Token new34(int line, int pos) {
      return new TRetn(line, pos);
   }

   Token new35(int line, int pos) {
      return new TDestruct(line, pos);
   }

   Token new36(int line, int pos) {
      return new TNot(line, pos);
   }

   Token new37(int line, int pos) {
      return new TDecisp(line, pos);
   }

   Token new38(int line, int pos) {
      return new TIncisp(line, pos);
   }

   Token new39(int line, int pos) {
      return new TJnz(line, pos);
   }

   Token new40(int line, int pos) {
      return new TCpdownbp(line, pos);
   }

   Token new41(int line, int pos) {
      return new TCptopbp(line, pos);
   }

   Token new42(int line, int pos) {
      return new TDecibp(line, pos);
   }

   Token new43(int line, int pos) {
      return new TIncibp(line, pos);
   }

   Token new44(int line, int pos) {
      return new TSavebp(line, pos);
   }

   Token new45(int line, int pos) {
      return new TRestorebp(line, pos);
   }

   Token new46(int line, int pos) {
      return new TStorestate(line, pos);
   }

   Token new47(int line, int pos) {
      return new TNop(line, pos);
   }

   Token new48(int line, int pos) {
      return new TT(line, pos);
   }

   Token new49(String text, int line, int pos) {
      return new TStringLiteral(text, line, pos);
   }

   Token new50(String text, int line, int pos) {
      return new TBlank(text, line, pos);
   }

   Token new51(String text, int line, int pos) {
      return new TIntegerConstant(text, line, pos);
   }

   Token new52(String text, int line, int pos) {
      return new TFloatConstant(text, line, pos);
   }

   private int getChar() throws IOException {
      if (this.eof) {
         return -1;
      } else {
         int result = this.in.read();
         if (result == -1) {
            this.eof = true;
         }

         return result;
      }
   }

   private void pushBack(int acceptLength) throws IOException {
      int length = this.text.length();

      for (int i = length - 1; i >= acceptLength; i--) {
         this.eof = false;
         this.in.unread(this.text.charAt(i));
      }
   }

   protected void unread(Token token) throws IOException {
      String text = token.getText();
      int length = text.length();

      for (int i = length - 1; i >= 0; i--) {
         this.eof = false;
         this.in.unread(text.charAt(i));
      }

      this.pos = token.getPos() - 1;
      this.line = token.getLine() - 1;
   }

   private String getText(int acceptLength) {
      StringBuffer s = new StringBuffer(acceptLength);

      for (int i = 0; i < acceptLength; i++) {
         s.append(this.text.charAt(i));
      }

      return s.toString();
   }

   public static class State {
      public static final Lexer.State INITIAL = new Lexer.State(0);
      private int id;

      private State(int id) {
         this.id = id;
      }

      public int id() {
         return this.id;
      }
   }
}

