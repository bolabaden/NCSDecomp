// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.StackEntry;
import java.util.ArrayList;
import java.util.List;

public class AFcnCallExp extends ScriptNode implements AExpression {
   private List<AExpression> params;
   private byte id;
   private StackEntry stackentry;

   public AFcnCallExp(byte id, List<AExpression> params) {
      this.id = id;
      this.params = new ArrayList<>();

      for (int i = 0; i < params.size(); i++) {
         this.addParam(params.get(i));
      }
   }

   protected void addParam(AExpression param) {
      param.parent(this);
      this.params.add(param);
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();
      buff.append("sub").append(Byte.toString(this.id)).append("(");
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

   @Override
   public void close() {
      super.close();
      if (this.params != null) {
         for (AExpression param : this.params) {
            ((ScriptNode)param).close();
         }

         this.params = null;
      }

      if (this.stackentry != null) {
         this.stackentry.close();
      }

      this.stackentry = null;
   }
}

