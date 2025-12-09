// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class AStoreStateCommand extends PStoreStateCommand {
   private TStorestate _storestate_;
   private TIntegerConstant _pos_;
   private TIntegerConstant _offset_;
   private TIntegerConstant _sizeBp_;
   private TIntegerConstant _sizeSp_;
   private TSemi _semi_;

   public AStoreStateCommand() {
   }

   public AStoreStateCommand(
      TStorestate _storestate_, TIntegerConstant _pos_, TIntegerConstant _offset_, TIntegerConstant _sizeBp_, TIntegerConstant _sizeSp_, TSemi _semi_
   ) {
      this.setStorestate(_storestate_);
      this.setPos(_pos_);
      this.setOffset(_offset_);
      this.setSizeBp(_sizeBp_);
      this.setSizeSp(_sizeSp_);
      this.setSemi(_semi_);
   }

   @Override
   public Object clone() {
      return new AStoreStateCommand(
         (TStorestate)this.cloneNode(this._storestate_),
         (TIntegerConstant)this.cloneNode(this._pos_),
         (TIntegerConstant)this.cloneNode(this._offset_),
         (TIntegerConstant)this.cloneNode(this._sizeBp_),
         (TIntegerConstant)this.cloneNode(this._sizeSp_),
         (TSemi)this.cloneNode(this._semi_)
      );
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAStoreStateCommand(this);
   }

   public TStorestate getStorestate() {
      return this._storestate_;
   }

   public void setStorestate(TStorestate node) {
      if (this._storestate_ != null) {
         this._storestate_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._storestate_ = node;
   }

   public TIntegerConstant getPos() {
      return this._pos_;
   }

   public void setPos(TIntegerConstant node) {
      if (this._pos_ != null) {
         this._pos_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._pos_ = node;
   }

   public TIntegerConstant getOffset() {
      return this._offset_;
   }

   public void setOffset(TIntegerConstant node) {
      if (this._offset_ != null) {
         this._offset_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._offset_ = node;
   }

   public TIntegerConstant getSizeBp() {
      return this._sizeBp_;
   }

   public void setSizeBp(TIntegerConstant node) {
      if (this._sizeBp_ != null) {
         this._sizeBp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._sizeBp_ = node;
   }

   public TIntegerConstant getSizeSp() {
      return this._sizeSp_;
   }

   public void setSizeSp(TIntegerConstant node) {
      if (this._sizeSp_ != null) {
         this._sizeSp_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._sizeSp_ = node;
   }

   public TSemi getSemi() {
      return this._semi_;
   }

   public void setSemi(TSemi node) {
      if (this._semi_ != null) {
         this._semi_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._semi_ = node;
   }

   @Override
   public String toString() {
      return this.toString(this._storestate_)
         + this.toString(this._pos_)
         + this.toString(this._offset_)
         + this.toString(this._sizeBp_)
         + this.toString(this._sizeSp_)
         + this.toString(this._semi_);
   }

   @Override
   void removeChild(Node child) {
      if (this._storestate_ == child) {
         this._storestate_ = null;
      } else if (this._pos_ == child) {
         this._pos_ = null;
      } else if (this._offset_ == child) {
         this._offset_ = null;
      } else if (this._sizeBp_ == child) {
         this._sizeBp_ = null;
      } else if (this._sizeSp_ == child) {
         this._sizeSp_ = null;
      } else if (this._semi_ == child) {
         this._semi_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._storestate_ == oldChild) {
         this.setStorestate((TStorestate)newChild);
      } else if (this._pos_ == oldChild) {
         this.setPos((TIntegerConstant)newChild);
      } else if (this._offset_ == oldChild) {
         this.setOffset((TIntegerConstant)newChild);
      } else if (this._sizeBp_ == oldChild) {
         this.setSizeBp((TIntegerConstant)newChild);
      } else if (this._sizeSp_ == oldChild) {
         this.setSizeSp((TIntegerConstant)newChild);
      } else if (this._semi_ == oldChild) {
         this.setSemi((TSemi)newChild);
      }
   }
}

