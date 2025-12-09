// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
# Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import com.kotor.resource.formats.ncs.stack.Variable;
import com.kotor.resource.formats.ncs.utils.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings({"unused"})
public class ASub extends ScriptRootNode {
   private Type type;
   private byte id;
   private List<ScriptNode> params;
   private String name;
   private boolean ismain;

   public ASub(Type type, byte id, List<AVarRef> params, int start, int end) {
      super(start, end);
      this.type = type;
      this.id = id;
      this.params = new ArrayList<>();
      this.tabs = "";

      for (int i = 0; i < params.size(); i++) {
         this.addParam(params.get(i));
      }

      this.name = "sub" + Byte.toString(id);
   }

   public ASub(int start, int end) {
      super(start, end);
      this.type = new Type((byte)0);
      this.params = null;
      this.tabs = "";
   }

   protected void addParam(AVarRef param) {
      param.parent(this);
      this.params.add(param);
   }

   @Override
   public String toString() {
      return this.getHeader() + " {" + this.newline + this.getBody() + "}" + this.newline;
   }

   public String getBody() {
      StringBuffer buff = new StringBuffer();

      for (int i = 0; i < this.children.size(); i++) {
         buff.append(this.children.get(i).toString());
      }

      return buff.toString();
   }

   public String getHeader() {
      StringBuffer buff = new StringBuffer();
      buff.append(this.type + " " + this.name + "(");
      String link = "";

      for (int i = 0; i < this.params.size(); i++) {
         AVarRef param = (AVarRef)this.params.get(i);
         Type ptype = param.type();
         buff.append(link + ptype + " " + param.toString());
         link = ", ";
      }

      buff.append(")");
      return buff.toString();
   }

   public void isMain(boolean ismain) {
      this.ismain = ismain;
      if (ismain) {
         if (this.type.equals((byte)3)) {
            this.name = "StartingConditional";
         } else {
            this.name = "main";
         }
      }
   }

   public boolean isMain() {
      return this.ismain;
   }

   public Type type() {
      return this.type;
   }

   public void name(String name) {
      this.name = name;
   }

   public String name() {
      return this.name;
   }

   public ArrayList<Variable> getParamVars() {
      ArrayList<Variable> vars = new ArrayList<>();
      if (this.params != null) {
         Iterator<ScriptNode> it = this.params.iterator();

         while (it.hasNext()) {
            vars.add(((AVarRef)it.next()).var());
         }
      }

      return vars;
   }

   @Override
   public void close() {
      super.close();
      if (this.params != null) {
         for (ScriptNode param : this.params) {
            param.close();
         }
      }

      this.params = null;
      if (this.type != null) {
         this.type.close();
      }

      this.type = null;
   }
}

