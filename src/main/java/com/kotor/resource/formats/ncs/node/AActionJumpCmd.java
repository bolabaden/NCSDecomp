// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AActionJumpCmd extends PCmd {
   private PStoreStateCommand _storeStateCommand_;
   private PJumpCommand _jumpCommand_;
   private PCommandBlock _commandBlock_;
   private PReturn _return_;

   public AActionJumpCmd() {
   }

   public AActionJumpCmd(PStoreStateCommand _storeStateCommand_, PJumpCommand _jumpCommand_, PCommandBlock _commandBlock_, PReturn _return_) {
      this.setStoreStateCommand(_storeStateCommand_);
      this.setJumpCommand(_jumpCommand_);
      this.setCommandBlock(_commandBlock_);
      this.setReturn(_return_);
   }

   @Override
   public Object clone() {
      return new AActionJumpCmd(
         (PStoreStateCommand)this.cloneNode(this._storeStateCommand_),
         (PJumpCommand)this.cloneNode(this._jumpCommand_),
         (PCommandBlock)this.cloneNode(this._commandBlock_),
         (PReturn)this.cloneNode(this._return_)
      );
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAActionJumpCmd(this);
   }

   public PStoreStateCommand getStoreStateCommand() {
      return this._storeStateCommand_;
   }

   public void setStoreStateCommand(PStoreStateCommand node) {
      if (this._storeStateCommand_ != null) {
         this._storeStateCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._storeStateCommand_ = node;
   }

   public PJumpCommand getJumpCommand() {
      return this._jumpCommand_;
   }

   public void setJumpCommand(PJumpCommand node) {
      if (this._jumpCommand_ != null) {
         this._jumpCommand_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._jumpCommand_ = node;
   }

   public PCommandBlock getCommandBlock() {
      return this._commandBlock_;
   }

   public void setCommandBlock(PCommandBlock node) {
      if (this._commandBlock_ != null) {
         this._commandBlock_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._commandBlock_ = node;
   }

   public PReturn getReturn() {
      return this._return_;
   }

   public void setReturn(PReturn node) {
      if (this._return_ != null) {
         this._return_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._return_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._storeStateCommand_) + this.toString(this._jumpCommand_) + this.toString(this._commandBlock_) + this.toString(this._return_);
   }

   @Override
   void removeChild(Node child) {
      if (this._storeStateCommand_ == child) {
         this._storeStateCommand_ = null;
      } else if (this._jumpCommand_ == child) {
         this._jumpCommand_ = null;
      } else if (this._commandBlock_ == child) {
         this._commandBlock_ = null;
      } else if (this._return_ == child) {
         this._return_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._storeStateCommand_ == oldChild) {
         this.setStoreStateCommand((PStoreStateCommand)newChild);
      } else if (this._jumpCommand_ == oldChild) {
         this.setJumpCommand((PJumpCommand)newChild);
      } else if (this._commandBlock_ == oldChild) {
         this.setCommandBlock((PCommandBlock)newChild);
      } else if (this._return_ == oldChild) {
         this.setReturn((PReturn)newChild);
      }
   }
}

