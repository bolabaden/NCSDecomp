// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ABinaryCmd extends PCmd {
   private PBinaryCommand _binaryCommand_;

   public ABinaryCmd() {
   }

   public ABinaryCmd(PBinaryCommand _binaryCommand_) {
      this.setBinaryCommand(_binaryCommand_);
   }

   @Override
   public Object clone() {
      return new ABinaryCmd((PBinaryCommand)this.cloneNode(this._binaryCommand_));
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseABinaryCmd(this);
   }

   public PBinaryCommand getBinaryCommand() {
      return this._binaryCommand_;
   }

   public void setBinaryCommand(PBinaryCommand node) {
      if (this._binaryCommand_ != null) {
         this._binaryCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._binaryCommand_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._binaryCommand_);
   }

   @Override
   void removeChild(Node child) {
      if (this._binaryCommand_ == child) {
         this._binaryCommand_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._binaryCommand_ == oldChild) {
         this.setBinaryCommand((PBinaryCommand)newChild);
      }
   }
}

