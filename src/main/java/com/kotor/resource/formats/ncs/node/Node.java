// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for all SableCC AST nodes; provides parent linkage, cloning, and helpers.
 */
public abstract class Node implements Switchable, Cloneable {
   private Node parent;

   @Override
   public abstract Object clone();

   public Node parent() {
      return this.parent;
   }

   void parent(Node parent) {
      this.parent = parent;
   }

   abstract void removeChild(Node var1);

   abstract void replaceChild(Node var1, Node var2);

   public void replaceBy(Node node) {
      if (this.parent != null) {
         this.parent.replaceChild(this, node);
      }
   }

   protected String toString(Node node) {
      return node != null ? node.toString() : "";
   }

   protected String toString(List<?> list) {
      StringBuffer s = new StringBuffer();
      Iterator<?> i = list.iterator();

      while (i.hasNext()) {
         s.append(i.next());
      }

      return s.toString();
   }

   protected Node cloneNode(Node node) {
      return node != null ? (Node)node.clone() : null;
   }

   protected <T extends Node> List<T> cloneList(List<T> list) {
      List<T> clone = new LinkedList<>();
      Iterator<T> i = list.iterator();

      while (i.hasNext()) {
         @SuppressWarnings("unchecked")
         T cloned = (T)i.next().clone();
         clone.add(cloned);
      }

      return clone;
   }
}

