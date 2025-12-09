// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;
import java.util.ArrayList;
import java.util.List;

/**
 * Script expression node representing a function/action call in NSS output.
 * Captures the action name, arguments, inferred stack entry for type info, and
 * the original action ID.
 */
public class AActionExp extends ScriptNode implements AExpression {
   private List<AExpression> params;
   private String action;
   private StackEntry stackentry;
   private int id;

   public AActionExp(String action, int id, List<AExpression> params) {
      this.action = action;
      this.params = new ArrayList<>();

      for (int i = 0; i < params.size(); i++) {
         this.addParam(params.get(i));
      }

      this.stackentry = null;
      this.id = id;
   }

   protected void addParam(AExpression param) {
      param.parent(this);
      this.params.add(param);
   }

   public AExpression getParam(int pos) {
      return this.params.get(pos);
   }

   public String action() {
      return this.action;
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();
      String prefix = "";

      for (int i = 0; i < this.params.size(); i++) {
         buff.append(prefix + this.params.get(i).toString());
         prefix = ", ";
      }

      buff.append(")");
      return buff.toString();
   }

   @Override
   public StackEntry stackentry() {
      return this.stackentry;
   }

   @Override
   public void stackentry(StackEntry stackentry) {
      this.stackentry = stackentry;
   }

   public int getId() {
      return this.id;
   }

   @Override
   public void close() {
      super.close();
      if (this.params != null) {
         for (AExpression param : this.params) {
            if (param instanceof ScriptNode) {
               ((ScriptNode)param).close();
            }
         }

         this.params = null;
      }

      if (this.stackentry != null) {
         this.stackentry.close();
      }

      this.stackentry = null;
   }
}

