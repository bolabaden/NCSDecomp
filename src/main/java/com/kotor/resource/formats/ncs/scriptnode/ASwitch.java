// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import java.util.ArrayList;
import java.util.List;

/**
 * Script-level AST node for a switch statement in emitted NSS code.
 * Holds the switch expression, ordered cases, optional default, and source span.
 */
public class ASwitch extends ScriptNode {
   protected AExpression switchexp;
   protected List<ASwitchCase> cases;
   protected ASwitchCase defaultcase;
   protected int start;
   protected int end;

   public ASwitch(int start, AExpression switchexp) {
      this.start = start;
      this.cases = new ArrayList<>();
      this.switchExp(switchexp);
   }

   public void switchExp(AExpression switchexp) {
      switchexp.parent(this);
      this.switchexp = switchexp;
   }

   public AExpression switchExp() {
      return this.switchexp;
   }

   public void end(int end) {
      this.end = end;
      if (this.defaultcase != null) {
         this.defaultcase.end(end);
      } else if (this.cases.size() > 0) {
         this.cases.get(this.cases.size() - 1).end(end);
      }
   }

   public int end() {
      return this.end;
   }

   public void addCase(ASwitchCase acase) {
      acase.parent(this);
      this.cases.add(acase);
   }

   public void addDefaultCase(ASwitchCase acase) {
      acase.parent(this);
      this.defaultcase = acase;
   }

   public ASwitchCase getLastCase() {
      return this.cases.get(this.cases.size() - 1);
   }

   public ASwitchCase getNextCase(ASwitchCase lastcase) {
      if (lastcase == null) {
         return this.getFirstCase();
      } else if (lastcase.equals(this.defaultcase)) {
         return null;
      } else {
         int index = this.cases.indexOf(lastcase) + 1;
         if (index == 0) {
            throw new RuntimeException("invalid last case passed in");
         } else {
            return this.cases.size() > index ? this.cases.get(index) : this.defaultcase;
         }
      }
   }

   public ASwitchCase getFirstCase() {
      return this.cases.size() > 0 ? this.cases.get(0) : this.defaultcase;
   }

   public int getFirstCaseStart() {
      if (this.cases.size() > 0) {
         return this.cases.get(0).getStart();
      } else {
         return this.defaultcase != null ? this.defaultcase.getStart() : -1;
      }
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();
      buff.append(this.tabs + "switch (" + this.switchexp + ") {" + this.newline);

      for (int i = 0; i < this.cases.size(); i++) {
         buff.append(this.cases.get(i).toString());
      }

      if (this.defaultcase != null) {
         buff.append(this.defaultcase.toString());
      }

      buff.append(this.tabs + "}" + this.newline);
      return buff.toString();
   }

   @Override
   public void close() {
      super.close();
      if (this.cases != null) {
         for (ScriptNode param : this.cases) {
            param.close();
         }

         this.cases = null;
      }

      if (this.switchexp != null) {
         ((ScriptNode)this.switchexp).close();
      }

      this.switchexp = null;
      if (this.defaultcase != null) {
         this.defaultcase.close();
      }

      this.defaultcase = null;
   }
}

