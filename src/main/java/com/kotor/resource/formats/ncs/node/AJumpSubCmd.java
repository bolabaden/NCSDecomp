// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AJumpSubCmd extends PCmd {
   private PJumpToSubroutine _jumpToSubroutine_;

   public AJumpSubCmd() {
   }

   public AJumpSubCmd(PJumpToSubroutine _jumpToSubroutine_) {
      this.setJumpToSubroutine(_jumpToSubroutine_);
   }

   @Override
   public Object clone() {
      return new AJumpSubCmd((PJumpToSubroutine)this.cloneNode(this._jumpToSubroutine_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAJumpSubCmd(this);
   }

   public PJumpToSubroutine getJumpToSubroutine() {
      return this._jumpToSubroutine_;
   }

   public void setJumpToSubroutine(PJumpToSubroutine node) {
      if (this._jumpToSubroutine_ != null) {
         this._jumpToSubroutine_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._jumpToSubroutine_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._jumpToSubroutine_);
   }

   @Override
   void removeChild(Node child) {
      if (this._jumpToSubroutine_ == child) {
         this._jumpToSubroutine_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._jumpToSubroutine_ == oldChild) {
         this.setJumpToSubroutine((PJumpToSubroutine)newChild);
      }
   }
}

