// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Composite type representing structs (and vectors as a special case).
 */
public class StructType extends Type {
   private ArrayList<Type> types = new ArrayList<>();
   private boolean alltyped = true;
   private String typename;
   private ArrayList<String> elements;

   public StructType() {
      super((byte)-15);
      this.size = 0;
   }

   @Override
   public void close() {
      if (this.types != null) {
         Iterator<Type> it = this.types.iterator();

         while (it.hasNext()) {
            it.next().close();
         }

         this.types = null;
      }

      this.elements = null;
   }

   public void print() {
      System.out.println("Struct has " + Integer.toString(this.types.size()) + " entries.");
      if (this.alltyped) {
         System.out.println("They have all been typed");
      } else {
         System.out.println("They have not all been typed");
      }

      for (int i = 0; i < this.types.size(); i++) {
         System.out.println("  Type: " + this.types.get(i).toString());
      }
   }

   public void addType(Type type) {
      this.types.add(type);
      if (type.equals(new Type((byte)-1))) {
         this.alltyped = false;
      }

      this.size = this.size + type.size();
   }

   public void addTypeStackOrder(Type type) {
      this.types.add(0, type);
      if (type.equals(new Type((byte)-1))) {
         this.alltyped = false;
      }

      this.size = this.size + type.size();
   }

   public boolean isVector() {
      if (this.size != 3) {
         return false;
      } else {
         for (int i = 0; i < 3; i++) {
            if (!(this.types.get(i)).equals((byte)4)) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public boolean isTyped() {
      return this.alltyped;
   }

   public void updateType(int pos, Type type) {
      this.types.set(pos, type);
      this.updateTyped();
   }

   public ArrayList<Type> types() {
      return this.types;
   }

   protected void updateTyped() {
      this.alltyped = true;

      for (int i = 0; i < this.types.size(); i++) {
         if (!this.types.get(i).isTyped()) {
            this.alltyped = false;
            return;
         }
      }
   }

   @Override
   public boolean equals(Object obj) {
      return !StructType.class.isInstance(obj) ? false : this.types.equals(((StructType)obj).types());
   }

   @Override
   public int hashCode() {
      return this.types.hashCode();
   }

   public void typeName(String name) {
      this.typename = name;
   }

   public String typeName() {
      return this.typename;
   }

   @Override
   public String toDeclString() {
      return this.isVector() ? Type.toString((byte)-16) : this.toString() + " " + this.typename;
   }

   public String elementName(int i) {
      if (this.elements == null) {
         this.setElementNames();
      }

      return this.elements.get(i);
   }

   @Override
   public Type getElement(int pos) {
      int remaining = pos;

      for (Type entry : this.types) {
         int size = entry.size();
         if (remaining <= size) {
            return entry.getElement(remaining);
         }
         remaining -= size;
      }

      throw new RuntimeException("Pos was greater than struct size");
   }

   private void setElementNames() {
      this.elements = new ArrayList<>();
      Hashtable<Type, Integer> typecounts = new Hashtable<>(1);
      if (this.isVector()) {
         this.elements.add("x");
         this.elements.add("y");
         this.elements.add("z");
      } else {
         for (int i = 0; i < this.types.size(); i++) {
            Type type = this.types.get(i);
            Integer typecount = typecounts.get(type);
            int count;
            if (typecount != null) {
               count = 1 + typecount;
            } else {
               count = 1;
            }

            this.elements.add(type.toString() + Integer.toString(count));
            typecounts.put(type, Integer.valueOf(count + 1));
         }
      }

      //Hashtable<Type, Integer> var6 = null;
   }
}

