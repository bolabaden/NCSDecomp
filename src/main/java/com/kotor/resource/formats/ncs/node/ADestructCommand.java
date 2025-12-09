// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;

public final class ADestructCommand extends PDestructCommand {
   private TDestruct _destruct_;
   private TIntegerConstant _pos_;
   private TIntegerConstant _type_;
   private TIntegerConstant _sizeRem_;
   private TIntegerConstant _offset_;
   private TIntegerConstant _sizeSave_;
   private TSemi _semi_;

   public ADestructCommand() {
   }

   public ADestructCommand(
      TDestruct _destruct_,
      TIntegerConstant _pos_,
      TIntegerConstant _type_,
      TIntegerConstant _sizeRem_,
      TIntegerConstant _offset_,
      TIntegerConstant _sizeSave_,
      TSemi _semi_
   ) {
      this.setDestruct(_destruct_);
      this.setPos(_pos_);
      this.setType(_type_);
      this.setSizeRem(_sizeRem_);
      this.setOffset(_offset_);
      this.setSizeSave(_sizeSave_);
      this.setSemi(_semi_);
   }

   @Override
   public Object clone() {
      return new ADestructCommand(
         (TDestruct)this.cloneNode(this._destruct_),
         (TIntegerConstant)this.cloneNode(this._pos_),
         (TIntegerConstant)this.cloneNode(this._type_),
         (TIntegerConstant)this.cloneNode(this._sizeRem_),
         (TIntegerConstant)this.cloneNode(this._offset_),
         (TIntegerConstant)this.cloneNode(this._sizeSave_),
         (TSemi)this.cloneNode(this._semi_)
      );
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseADestructCommand(this);
   }

   public TDestruct getDestruct() {
      return this._destruct_;
   }

   public void setDestruct(TDestruct node) {
      if (this._destruct_ != null) {
         this._destruct_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._destruct_ = node;
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

   public TIntegerConstant getType() {
      return this._type_;
   }

   public void setType(TIntegerConstant node) {
      if (this._type_ != null) {
         this._type_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._type_ = node;
   }

   public TIntegerConstant getSizeRem() {
      return this._sizeRem_;
   }

   public void setSizeRem(TIntegerConstant node) {
      if (this._sizeRem_ != null) {
         this._sizeRem_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._sizeRem_ = node;
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

   public TIntegerConstant getSizeSave() {
      return this._sizeSave_;
   }

   public void setSizeSave(TIntegerConstant node) {
      if (this._sizeSave_ != null) {
         this._sizeSave_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._sizeSave_ = node;
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
      return this.toString(this._destruct_)
         + this.toString(this._pos_)
         + this.toString(this._type_)
         + this.toString(this._sizeRem_)
         + this.toString(this._offset_)
         + this.toString(this._sizeSave_)
         + this.toString(this._semi_);
   }

   @Override
   void removeChild(Node child) {
      if (this._destruct_ == child) {
         this._destruct_ = null;
      } else if (this._pos_ == child) {
         this._pos_ = null;
      } else if (this._type_ == child) {
         this._type_ = null;
      } else if (this._sizeRem_ == child) {
         this._sizeRem_ = null;
      } else if (this._offset_ == child) {
         this._offset_ = null;
      } else if (this._sizeSave_ == child) {
         this._sizeSave_ = null;
      } else if (this._semi_ == child) {
         this._semi_ = null;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._destruct_ == oldChild) {
         this.setDestruct((TDestruct)newChild);
      } else if (this._pos_ == oldChild) {
         this.setPos((TIntegerConstant)newChild);
      } else if (this._type_ == oldChild) {
         this.setType((TIntegerConstant)newChild);
      } else if (this._sizeRem_ == oldChild) {
         this.setSizeRem((TIntegerConstant)newChild);
      } else if (this._offset_ == oldChild) {
         this.setOffset((TIntegerConstant)newChild);
      } else if (this._sizeSave_ == oldChild) {
         this.setSizeSave((TIntegerConstant)newChild);
      } else if (this._semi_ == oldChild) {
         this.setSemi((TSemi)newChild);
      }
   }
}

