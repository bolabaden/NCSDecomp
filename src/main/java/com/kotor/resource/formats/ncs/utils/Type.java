// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

/**
 * Represents an NWScript value type and its stack footprint (in 4-byte slots).
 * Encodes primitive and composite types using nwnnsscomp byte codes.
 */
public class Type {
   public static final byte VT_NONE = 0;
   public static final byte VT_STACK = 1;
   public static final byte VT_INTEGER = 3;
   public static final byte VT_FLOAT = 4;
   public static final byte VT_STRING = 5;
   public static final byte VT_OBJECT = 6;
   public static final byte VT_EFFECT = 16;
   public static final byte VT_EVENT = 17;
   public static final byte VT_LOCATION = 18;
   public static final byte VT_TALENT = 19;
   public static final byte VT_INTINT = 32;
   public static final byte VT_FLOATFLOAT = 33;
   public static final byte VT_OBJECTOBJECT = 34;
   public static final byte VT_STRINGSTRING = 35;
   public static final byte VT_STRUCTSTRUCT = 36;
   public static final byte VT_INTFLOAT = 37;
   public static final byte VT_FLOATINT = 38;
   public static final byte VT_EFFECTEFFECT = 48;
   public static final byte VT_EVENTEVENT = 49;
   public static final byte VT_LOCLOC = 50;
   public static final byte VT_TALTAL = 51;
   public static final byte VT_VECTORVECTOR = 58;
   public static final byte VT_VECTORFLOAT = 59;
   public static final byte VT_FLOATVECTOR = 60;
   public static final byte VT_VECTOR = -16;
   public static final byte VT_STRUCT = -15;
   public static final byte VT_INVALID = -1;
   protected byte type;
   protected int size;

   public Type(byte type) {
      this.type = type;
      this.size = 1;
   }

   public Type(String str) {
      this.type = decode(str);
      this.size = typeSize(this.type) / 4;
   }

   public static Type parseType(String str) {
      return new Type(str);
   }

   public void close() {
   }

   public byte byteValue() {
      return this.type;
   }

   @Override
   public String toString() {
      return toString(this.type);
   }

   public static String toString(Type atype) {
      return toString(atype.type);
   }

   public String toDeclString() {
      return this.toString();
   }

   public int size() {
      return this.size;
   }

   public boolean isTyped() {
      return this.type != -1;
   }

   public String toValueString() {
      return Byte.toString(this.type);
   }

   protected static String toString(byte type) {
      switch (type) {
         case -16:
            return "vector";
         case -15:
            return "struct";
         case -1:
            return "invalid";
         case 0:
            return "void";
         case 1:
            return "stack";
         case 3:
            return "int";
         case 4:
            return "float";
         case 5:
            return "string";
         case 6:
            return "object";
         case 16:
            return "effect";
         case 18:
            return "location";
         case 19:
            return "talent";
         case 32:
            return "intint";
         case 33:
            return "floatfloat";
         case 34:
            return "objectobject";
         case 35:
            return "stringstring";
         case 36:
            return "structstruct";
         case 37:
            return "intfloat";
         case 38:
            return "floatint";
         case 48:
            return "effecteffect";
         case 49:
            return "eventevent";
         case 50:
            return "locloc";
         case 51:
            return "taltal";
         case 58:
            return "vectorvector";
         case 59:
            return "vectorfloat";
         case 60:
            return "floatvector";
         default:
            return "unknown";
      }
   }

   private static byte decode(String type) {
      if (type.equals("void")) {
         return 0;
      } else if (type.equals("int")) {
         return 3;
      } else if (type.equals("float")) {
         return 4;
      } else if (type.equals("string")) {
         return 5;
      } else if (type.equals("object")) {
         return 6;
      } else if (type.equals("effect")) {
         return 16;
      } else if (type.equals("event")) {
         return 17;
      } else if (type.equals("location")) {
         return 18;
      } else if (type.equals("talent")) {
         return 19;
      } else if (type.equals("vector")) {
         return -16;
      } else if (type.equals("action")) {
         return 0;
      } else if (type.equals("INT")) {
         return 3;
      } else if (type.equals("OBJECT_ID")) {
         return 6;
      } else {
         throw new RuntimeException("Attempted to get unknown type " + type);
      }
   }

   public int typeSize() {
      return typeSize(this.type);
   }

   public static int typeSize(String type) {
      return typeSize(decode(type));
   }

   private static int typeSize(byte type) {
      switch (type) {
         case -16:
            return 12;
         case 0:
            return 0;
         case 3:
            return 4;
         case 4:
            return 4;
         case 5:
            return 4;
         case 6:
            return 4;
         case 16:
            return 4;
         case 17:
            return 4;
         case 18:
            return 4;
         case 19:
            return 4;
         default:
            throw new RuntimeException("Unknown type code: " + Byte.toString(type));
      }
   }

   public Type getElement(int pos) {
      if (pos != 1) {
         throw new RuntimeException("Position > 1 for type, not struct");
      } else {
         return this;
      }
   }

   @Override
   public boolean equals(Object obj) {
      return Type.class.isInstance(obj) ? this.type == ((Type)obj).type : false;
   }

   public boolean equals(byte type) {
      return this.type == type;
   }

   @Override
   public int hashCode() {
      return this.type;
   }
}

