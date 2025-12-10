// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptnode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for script-level nodes that own child statements/expressions,
 * tracking start/end positions and providing child management helpers.
 */
public abstract class ScriptRootNode extends ScriptNode {
   protected LinkedList<ScriptNode> children = new LinkedList<>();
   protected int start;
   protected int end;

   protected ScriptRootNode(int start, int end) {
      this.start = start;
      this.end = end;
   }

   public void addChild(ScriptNode child) {
      this.children.add(child);
      child.parent(this);
   }

   public void addChildren(List<? extends ScriptNode> children) {
      Iterator<? extends ScriptNode> it = children.iterator();

      while (it.hasNext()) {
         this.addChild(it.next());
      }
   }

   public ArrayList<ScriptNode> removeChildren(int first, int last) {
      ArrayList<ScriptNode> removed = new ArrayList<>(last - first + 1);

      for (int i = 0; i <= last - first; i++) {
         removed.add(this.removeChild(first));
      }

      return removed;
   }

   public ArrayList<ScriptNode> removeChildren(int first) {
      return this.removeChildren(first, this.children.size() - 1);
   }

   public ArrayList<ScriptNode> removeChildren() {
      return this.removeChildren(0, this.children.size() - 1);
   }

   public ScriptNode removeLastChild() {
      return this.children.removeLast();
   }

   public void removeChild(ScriptNode child) {
      this.children.remove(child);
      child.parent(null);
   }

   public ScriptNode removeChild(int index) {
      ScriptNode child = this.children.remove(index);
      child.parent(null);
      return child;
   }

   public ScriptNode getLastChild() {
      return this.children.getLast();
   }

   public ScriptNode getPreviousChild(int pos) {
      return this.children.size() < pos ? null : this.children.get(this.children.size() - pos);
   }

   public boolean hasChildren() {
      return this.children.size() > 0;
   }

   public int getEnd() {
      return this.end;
   }

   public int getStart() {
      return this.start;
   }

   public LinkedList<ScriptNode> getChildren() {
      return this.children;
   }

   public int getChildLocation(ScriptNode child) {
      return this.children.indexOf(child);
   }

   public void replaceChild(ScriptNode oldchild, ScriptNode newchild) {
      int index = this.getChildLocation(oldchild);
      this.children.set(index, newchild);
      newchild.parent(this);
      oldchild.parent(null);
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();

      for (int i = 0; i < this.children.size(); i++) {
         buff.append(this.children.get(i).toString());
      }

      return buff.toString();
   }

   public int size() {
      return this.children.size();
   }

   @Override
   public void close() {
      super.close();
      Iterator<ScriptNode> it = this.children.iterator();

      while (it.hasNext()) {
         it.next().close();
      }

      this.children = null;
   }
}

