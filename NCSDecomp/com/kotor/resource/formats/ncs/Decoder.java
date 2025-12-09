// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Decodes compiled NCS bytecode into a tokenized command stream.
 * <p>
 * This class reads the binary instruction set, validates the file header, and
 * emits a flat string representation consumed by the parser. It is intentionally
 * low-level: all branching/stack semantics are handled by later passes.
 */
@SuppressWarnings("unused")
public class Decoder {
   private static final int DECOCT_CPDOWNSP = 1;
   private static final int DECOCT_RSADD = 2;
   private static final int DECOCT_CPTOPSP = 3;
   private static final int DECOCT_CONST = 4;
   private static final int DECOCT_ACTION = 5;
   private static final int DECOCT_LOGANDII = 6;
   private static final int DECOCT_LOGORII = 7;
   private static final int DECOCT_INCORII = 8;
   private static final int DECOCT_EXCORII = 9;
   private static final int DECOCT_BOOLANDII = 10;
   private static final int DECOCT_EQUAL = 11;
   private static final int DECOCT_NEQUAL = 12;
   private static final int DECOCT_GEQ = 13;
   private static final int DECOCT_GT = 14;
   private static final int DECOCT_LT = 15;
   private static final int DECOCT_LEQ = 16;
   private static final int DECOCT_SHLEFTII = 17;
   private static final int DECOCT_SHRIGHTII = 18;
   private static final int DECOCT_USHRIGHTII = 19;
   private static final int DECOCT_ADD = 20;
   private static final int DECOCT_SUB = 21;
   private static final int DECOCT_MUL = 22;
   private static final int DECOCT_DIV = 23;
   private static final int DECOCT_MOD = 24;
   private static final int DECOCT_NEG = 25;
   private static final int DECOCT_COMP = 26;
   private static final int DECOCT_MOVSP = 27;
   private static final int DECOCT_STATEALL = 28;
   private static final int DECOCT_JMP = 29;
   private static final int DECOCT_JSR = 30;
   private static final int DECOCT_JZ = 31;
   private static final int DECOCT_RETN = 32;
   private static final int DECOCT_DESTRUCT = 33;
   private static final int DECOCT_NOT = 34;
   private static final int DECOCT_DECISP = 35;
   private static final int DECOCT_INCISP = 36;
   private static final int DECOCT_JNZ = 37;
   private static final int DECOCT_CPDOWNBP = 38;
   private static final int DECOCT_CPTOPBP = 39;
   private static final int DECOCT_DECIBP = 40;
   private static final int DECOCT_INCIBP = 41;
   private static final int DECOCT_SAVEBP = 42;
   private static final int DECOCT_RESTOREBP = 43;
   private static final int DECOCT_STORE_STATE = 44;
   private static final int DECOCT_NOP = 45;
   private static final int DECOCT_T = 66;
   /** Input stream positioned at the start of an NCS file. */
   private BufferedInputStream in;
   /** Reference action table used when decoding ACTION opcodes. */
   private ActionsData actions;
   /** Current byte position within the stream for error reporting. */
   private int pos;

   /**
    * @param in Input stream containing the NCS content
    * @param actions Parsed actions table for opcode lookup
    */
   public Decoder(BufferedInputStream in, ActionsData actions) {
      this.in = in;
      this.actions = actions;
      this.pos = 0;
   }

   /**
    * Reads and decodes the entire NCS file.
    *
    * @return Tokenized command string used by the SableCC parser
    */
   public String decode() throws IOException, Exception {
      this.readHeader();
      return this.readCommands();
   }

   private String readCommands() throws IOException, Exception {
      StringBuffer strbuffer = new StringBuffer();

      while (this.readCommand(strbuffer) != -1) {
      }

      return strbuffer.toString();
   }

   private int readCommand(StringBuffer strbuffer) throws IOException, Exception {
      byte[] buffer = new byte[1];
      int commandpos = this.pos;
      int status = this.in.read(buffer, 0, 1);
      this.pos++;
      if (status == -1) {
         return status;
      } else {
         strbuffer.append(this.getCommand(buffer[0]));
         strbuffer.append(" " + Integer.toString(commandpos));

         try {
            label33:
            switch (buffer[0]) {
               case 1:
               case 3:
               case 38:
               case 39:
                  strbuffer.append(" " + this.readByteAsString());
                  strbuffer.append(" " + this.readSignedInt());
                  strbuffer.append(" " + this.readUnsignedShort());
                  break;
               case 2:
               case 6:
               case 7:
               case 8:
               case 9:
               case 10:
               case 13:
               case 14:
               case 15:
               case 16:
               case 17:
               case 18:
               case 19:
               case 20:
               case 21:
               case 22:
               case 23:
               case 24:
               case 25:
               case 26:
               case 32:
               case 34:
               case 42:
               case 43:
               case 45:
                  strbuffer.append(" " + this.readByteAsString());
                  break;
               case 4:
                  byte bx = this.readByte();
                  strbuffer.append(" " + Byte.toString(bx));
                  switch (bx) {
                     case 3:
                        strbuffer.append(" " + this.readUnsignedInt());
                        break label33;
                     case 4:
                        strbuffer.append(" " + this.readFloat());
                        break label33;
                     case 5:
                        strbuffer.append(" " + this.readString());
                        break label33;
                     case 6:
                        strbuffer.append(" " + this.readSignedInt());
                        break label33;
                     default:
                        throw new RuntimeException("Unknown or unexpected constant type: " + Byte.toString(bx));
                  }
               case 5:
                  strbuffer.append(" " + this.readByteAsString());
                  strbuffer.append(" " + this.readUnsignedShort());
                  strbuffer.append(" " + this.readByteAsString());
                  break;
               case 11:
               case 12:
                  byte b = this.readByte();
                  strbuffer.append(" " + Byte.toString(b));
                  if (b == 36) {
                     strbuffer.append(" " + this.readUnsignedShort());
                  }
                  break;
               case 27:
               case 29:
               case 30:
               case 31:
               case 35:
               case 36:
               case 37:
               case 40:
               case 41:
                  strbuffer.append(" " + this.readByteAsString());
                  strbuffer.append(" " + this.readSignedInt());
                  break;
               case 28:
                  strbuffer.append(" " + this.readByteAsString());
                  break;
               case 33:
                  strbuffer.append(" " + this.readByteAsString());
                  strbuffer.append(" " + this.readUnsignedShort());
                  strbuffer.append(" " + this.readUnsignedShort());
                  strbuffer.append(" " + this.readUnsignedShort());
                  break;
               case 44:
                  strbuffer.append(" " + this.readByteAsString());
                  strbuffer.append(" " + this.readSignedInt());
                  strbuffer.append(" " + this.readSignedInt());
                  break;
               case 46:
               case 47:
               case 48:
               case 49:
               case 50:
               case 51:
               case 52:
               case 53:
               case 54:
               case 55:
               case 56:
               case 57:
               case 58:
               case 59:
               case 60:
               case 61:
               case 62:
               case 63:
               case 64:
               case 65:
               default:
                  throw new RuntimeException("Unknown command type: " + Byte.toString(buffer[0]));
               case 66:
                  strbuffer.append(" " + this.readSignedInt());
            }
         } catch (Exception var7) {
            System.out.println("error in .ncs file at pos " + Integer.toString(this.pos));
            throw var7;
         }

         strbuffer.append("; ");
         return 1;
      }
   }

   private byte readByte() throws IOException, Exception {
      byte[] buffer = new byte[1];
      int status = this.in.read(buffer, 0, 1);
      this.pos++;
      if (status == -1) {
         throw new RuntimeException("Unexpected EOF");
      } else {
         return buffer[0];
      }
   }

   private String readByteAsString() throws IOException, Exception {
      return Byte.toString(this.readByte());
   }

   private String readUnsignedInt() throws IOException, Exception {
      byte[] buffer = new byte[4];
      int status = this.in.read(buffer, 0, 4);
      if (status == -1) {
         throw new RuntimeException("Unexpected EOF");
      } else {
         this.pos += 4;
         long l = 0L;

         for (int i = 0; i < 4; i++) {
            l |= buffer[i] & 255;
            if (i < 3) {
               l <<= 8;
            }
         }

         return Long.toString(l);
      }
   }

   private String readSignedInt() throws IOException, Exception {
      byte[] buffer = new byte[4];
      int status = this.in.read(buffer, 0, 4);
      if (status == -1) {
         throw new RuntimeException("Unexpected EOF");
      } else {
         this.pos += 4;
         BigInteger i = new BigInteger(buffer);
         return i.toString();
      }
   }

   private String readUnsignedShort() throws IOException, Exception {
      byte[] buffer = new byte[2];
      int status = this.in.read(buffer, 0, 2);
      if (status == -1) {
         throw new RuntimeException("Unexpected EOF");
      } else {
         this.pos += 2;
         BigInteger i = new BigInteger(buffer);
         return i.toString();
      }
   }

   private String readFloat() throws IOException, Exception {
      byte[] buffer = new byte[4];
      int status = this.in.read(buffer, 0, 4);
      if (status == -1) {
         throw new RuntimeException("Unexpected EOF");
      } else {
         this.pos += 4;
         BigInteger i = new BigInteger(buffer);
         return Float.toString(Float.intBitsToFloat(i.intValue()));
      }
   }

   private String readString() throws IOException, Exception {
      byte[] buffer = new byte[2];
      int status = this.in.read(buffer, 0, 2);
      if (status == -1) {
         throw new RuntimeException("Unexpected EOF");
      } else {
         this.pos += 2;
         int size = new BigInteger(buffer).intValue();
         buffer = new byte[size];
         status = this.in.read(buffer, 0, size);
         if (status == -1) {
            throw new RuntimeException("Unexpected EOF");
         } else {
            this.pos += size;
            return "\"" + new String(buffer) + "\"";
         }
      }
   }

   private void readHeader() throws IOException, Exception {
      byte[] buffer = new byte[8];
      byte[] header = new byte[]{78, 67, 83, 32, 86, 49, 46, 48};
      this.in.read(buffer, 0, 8);
      this.pos += 8;
      if (!Arrays.equals(buffer, header)) {
         throw new RuntimeException("The data file is not an NCS V1.0 file.");
      }
   }

   private String getCommand(byte command) throws Exception {
      switch (command) {
         case 1:
            return "CPDOWNSP";
         case 2:
            return "RSADD";
         case 3:
            return "CPTOPSP";
         case 4:
            return "CONST";
         case 5:
            return "ACTION";
         case 6:
            return "LOGANDII";
         case 7:
            return "LOGORII";
         case 8:
            return "INCORII";
         case 9:
            return "EXCORII";
         case 10:
            return "BOOLANDII";
         case 11:
            return "EQUAL";
         case 12:
            return "NEQUAL";
         case 13:
            return "GEQ";
         case 14:
            return "GT";
         case 15:
            return "LT";
         case 16:
            return "LEQ";
         case 17:
            return "SHLEFTII";
         case 18:
            return "SHRIGHTII";
         case 19:
            return "USHRIGHTII";
         case 20:
            return "ADD";
         case 21:
            return "SUB";
         case 22:
            return "MUL";
         case 23:
            return "DIV";
         case 24:
            return "MOD";
         case 25:
            return "NEG";
         case 26:
            return "COMP";
         case 27:
            return "MOVSP";
         case 28:
            return "STATEALL";
         case 29:
            return "JMP";
         case 30:
            return "JSR";
         case 31:
            return "JZ";
         case 32:
            return "RETN";
         case 33:
            return "DESTRUCT";
         case 34:
            return "NOT";
         case 35:
            return "DECISP";
         case 36:
            return "INCISP";
         case 37:
            return "JNZ";
         case 38:
            return "CPDOWNBP";
         case 39:
            return "CPTOPBP";
         case 40:
            return "DECIBP";
         case 41:
            return "INCIBP";
         case 42:
            return "SAVEBP";
         case 43:
            return "RESTOREBP";
         case 44:
            return "STORE_STATE";
         case 45:
            return "NOP";
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 64:
         case 65:
         default:
            throw new RuntimeException("Unknown command code: " + Byte.toString(command));
         case 66:
            return "T";
      }
   }
}

