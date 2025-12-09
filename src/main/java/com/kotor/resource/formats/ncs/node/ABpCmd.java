// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

/**
 * AST command wrapper for BP stack operations in bytecode.
 */
public final class ABpCmd extends PCmd {
   private PBpCommand _bpCommand_;

   public ABpCmd() {
   }

   public ABpCmd(PBpCommand _bpCommand_) {
      this.setBpCommand(_bpCommand_);
   }

   @Override
   public Object clone() {
      return new ABpCmd((PBpCommand)this.cloneNode(this._bpCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseABpCmd(this);
   }

   public PBpCommand getBpCommand() {
      return this._bpCommand_;
   }

   public void setBpCommand(PBpCommand node) {
      if (this._bpCommand_ != null) {
         this._bpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._bpCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._bpCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._bpCommand_ == child) {
         this._bpCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._bpCommand_ == oldChild) {
         this.setBpCommand((PBpCommand)newChild);
      }
   }
}

