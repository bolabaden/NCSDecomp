// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

/**
 * AST command wrapper for stack operations (SP/BP) in bytecode.
 */
public final class AStackOpCmd extends PCmd {
   private PStackCommand _stackCommand_;

   public AStackOpCmd() {
   }

   public AStackOpCmd(PStackCommand _stackCommand_) {
      this.setStackCommand(_stackCommand_);
   }

   @Override
   public Object clone() {
      return new AStackOpCmd((PStackCommand)this.cloneNode(this._stackCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAStackOpCmd(this);
   }

   public PStackCommand getStackCommand() {
      return this._stackCommand_;
   }

   public void setStackCommand(PStackCommand node) {
      if (this._stackCommand_ != null) {
         this._stackCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._stackCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._stackCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._stackCommand_ == child) {
         this._stackCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._stackCommand_ == oldChild) {
         this.setStackCommand((PStackCommand)newChild);
      }
   }
}

