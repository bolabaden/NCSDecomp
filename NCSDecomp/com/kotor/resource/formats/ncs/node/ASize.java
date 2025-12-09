// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ASize extends PSize {
   private TT _t_;
   private TIntegerConstant _pos_;
   private TIntegerConstant _integerConstant_;
   private TSemi _semi_;

   public ASize() {
   }

   public ASize(TT _t_, TIntegerConstant _pos_, TIntegerConstant _integerConstant_, TSemi _semi_) {
      this.setT(_t_);
      this.setPos(_pos_);
      this.setIntegerConstant(_integerConstant_);
      this.setSemi(_semi_);
   }

   @Override
   public Object clone() {
      return new ASize(
         (TT)this.cloneNode(this._t_),
         (TIntegerConstant)this.cloneNode(this._pos_),
         (TIntegerConstant)this.cloneNode(this._integerConstant_),
         (TSemi)this.cloneNode(this._semi_)
      );
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseASize(this);
   }

   public TT getT() {
      return this._t_;
   }

   public void setT(TT node) {
      if (this._t_ != null) {
         this._t_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._t_ = node;
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

   public TIntegerConstant getIntegerConstant() {
      return this._integerConstant_;
   }

   public void setIntegerConstant(TIntegerConstant node) {
      if (this._integerConstant_ != null) {
         this._integerConstant_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._integerConstant_ = node;
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
      return this.toString(this._t_) + this.toString(this._pos_) + this.toString(this._integerConstant_) + this.toString(this._semi_);
   }

   @Override
   void removeChild(Node child) {
      if (this._t_ == child) {
         this._t_ = null;
      } else if (this._pos_ == child) {
         this._pos_ = null;
      } else if (this._integerConstant_ == child) {
         this._integerConstant_ = null;
      } else if (this._semi_ == child) {
         this._semi_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._t_ == oldChild) {
         this.setT((TT)newChild);
      } else if (this._pos_ == oldChild) {
         this.setPos((TIntegerConstant)newChild);
      } else if (this._integerConstant_ == oldChild) {
         this.setIntegerConstant((TIntegerConstant)newChild);
      } else if (this._semi_ == oldChild) {
         this.setSemi((TSemi)newChild);
      }
   }
}

