// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import com.kotor.resource.formats.ncs.utils.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and exposes the action table from the {@code *_nwscript.nss} files.
 * <p>
 * The table describes every engine action: name, return type, and parameter
 * types. Decompilation and type analysis use this metadata to size the stack
 * correctly and emit meaningful prototypes.
 */
public class ActionsData {
   /** Ordered list of parsed actions (index matches opcode value). */
   private final List<Action> actions;
   /** Reader over the nwscript actions block. */
   private final BufferedReader actionsreader;

   /**
    * Reads the actions table from the provided reader.
    *
    * @param actionsreader Reader positioned anywhere in the nwscript file
    * @throws IOException if the table cannot be parsed
    */
   public ActionsData(BufferedReader actionsreader) throws IOException {
      this.actionsreader = actionsreader;
      this.actions = new ArrayList<>(877);
      this.readActions();
   }

   /**
    * Returns the serialized representation of an action by index.
    *
    * @param index opcode-style index into the actions table
    * @return quoted name plus return type and parameter size
    */
   public String getAction(int index) {
      try {
         ActionsData.Action action = this.actions.get(index);
         return action.toString();
      } catch (IndexOutOfBoundsException var3) {
         throw new RuntimeException("Invalid action call: action " + Integer.toString(index));
      }
   }

   /**
    * Parses the action table, starting at the first {@code // 0} marker.
    */
   private void readActions() throws IOException {
      Pattern p = Pattern.compile("^\\s*(\\w+)\\s+(\\w+)\\s*\\((.*)\\).*");
      String str;
      while ((str = this.actionsreader.readLine()) != null && !str.startsWith("// 0")) {
      }

      while ((str = this.actionsreader.readLine()) != null) {
         if (!str.startsWith("//") && str.length() != 0) {
            Matcher m = p.matcher(str);
            if (m.matches()) {
               this.actions.add(new ActionsData.Action(m.group(1), m.group(2), m.group(3)));
            }
         }
      }

      System.out.println("read actions.  There were " + Integer.toString(this.actions.size()));
   }

   public Type getReturnType(int index) {
      if (index < 0 || index >= this.actions.size()) {
         throw new RuntimeException("Invalid action index: " + index + " (actions list size: " + this.actions.size() + ")");
      }
      return this.actions.get(index).returnType();
   }

   public String getName(int index) {
      if (index < 0 || index >= this.actions.size()) {
         throw new RuntimeException("Invalid action index: " + index + " (actions list size: " + this.actions.size() + ")");
      }
      return this.actions.get(index).name();
   }

   public List<Type> getParamTypes(int index) {
      if (index < 0 || index >= this.actions.size()) {
         throw new RuntimeException("Invalid action index: " + index + " (actions list size: " + this.actions.size() + ")");
      }
      return this.actions.get(index).params();
   }

   /**
    * Immutable record of a single action signature.
    */
   public static class Action {
      private final String name;
      private final Type returntype;
      private int paramsize;
      private final List<Type> paramlist;

      /**
       * Parses a signature line such as {@code void ActionName(int param)}.
       *
       * @param type Return type string
       * @param name Action name
       * @param params Raw parameter list string
       */
      public Action(String type, String name, String params) {
         this.name = name;
         this.returntype = Type.parseType(type);
         this.paramlist = new ArrayList<>();
         this.paramsize = 0;
         Pattern p = Pattern.compile("\\s*(\\w+)\\s+\\w+(\\s*=\\s*\\S+)?\\s*");
         String[] tokens = params.split(",");

         for (int i = 0; i < tokens.length; i++) {
            Matcher m = p.matcher(tokens[i]);
            if (m.matches()) {
               this.paramlist.add(new Type(m.group(1)));
               this.paramsize = this.paramsize + Type.typeSize(m.group(1));
            }
         }
      }

      @Override
      public String toString() {
         return "\"" + this.name + "\" " + this.returntype.toValueString() + " " + Integer.toString(this.paramsize);
      }

      /** Parameter types in declaration order. */
      public List<Type> params() {
         return this.paramlist;
      }

      /** Return type of the action. */
      public Type returnType() {
         return this.returntype;
      }

      /** Total stack size consumed by parameters. */
      public int paramsize() {
         return this.paramsize;
      }

      /** Action name. */
      public String name() {
         return this.name;
      }
   }
}

