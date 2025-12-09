// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * SableCC node that holds an ordered list of commands within a subroutine.
 */
public final class ACommandBlock extends PCommandBlock {
   private final LinkedList<PCmd> _cmd_ = new TypedLinkedList<PCmd>(new ACommandBlock.Cmd_Cast());

   public ACommandBlock() {
   }

   public ACommandBlock(List<? extends PCmd> _cmd_) {
      this._cmd_.clear();
      this._cmd_.addAll(_cmd_);
   }

   public ACommandBlock(XPCmd _cmd_) {
      if (_cmd_ != null) {
         while (_cmd_ instanceof X1PCmd) {
            this._cmd_.addFirst(((X1PCmd)_cmd_).getPCmd());
            _cmd_ = ((X1PCmd)_cmd_).getXPCmd();
         }

         this._cmd_.addFirst(((X2PCmd)_cmd_).getPCmd());
      }
   }

   @Override
   public Object clone() {
      return new ACommandBlock(this.cloneList(this._cmd_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseACommandBlock(this);
   }

   public LinkedList<PCmd> getCmd() {
      return this._cmd_;
   }

   public void setCmd(List<PCmd> list) {
      this._cmd_.clear();
      this._cmd_.addAll(list);
   }

   @Override
   public String toString() {
      return this.toString(this._cmd_);
   }

   @Override
   void removeChild(Node child) {
      if (!this._cmd_.remove(child)) {
         ;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      ListIterator<PCmd> i = this._cmd_.listIterator();

      while (i.hasNext()) {
         if (i.next() == oldChild) {
            if (newChild != null) {
               i.set((PCmd)newChild);
               oldChild.parent(null);
               return;
            }

            i.remove();
            oldChild.parent(null);
            return;
         }
      }
   }

   private class Cmd_Cast implements Cast<PCmd> {
      private static final long serialVersionUID = 1L;
      Cmd_Cast() {
      }

      @Override
      public PCmd cast(Object o) {
         PCmd node = (PCmd)o;
         if (node.parent() != null && node.parent() != ACommandBlock.this) {
            node.parent().removeChild(node);
         }

         if (node.parent() == null || node.parent() != ACommandBlock.this) {
            node.parent(ACommandBlock.this);
         }

         return node;
      }
   }
}

