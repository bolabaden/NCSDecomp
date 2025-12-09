// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.node;

import com.kotor.resource.formats.ncs.analysis.Analysis;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * SableCC node representing the program root; contains subroutines and optional prolog.
 */
public final class AProgram extends PProgram {
   private PSize _size_;
   private PRsaddCommand _conditional_;
   private PJumpToSubroutine _jumpToSubroutine_;
   private PReturn _return_;
   private final LinkedList<PSubroutine> _subroutine_ = new TypedLinkedList<PSubroutine>(new AProgram.Subroutine_Cast());

   public AProgram() {
   }

   public AProgram(PSize _size_, PRsaddCommand _conditional_, PJumpToSubroutine _jumpToSubroutine_, PReturn _return_, List<? extends PSubroutine> _subroutine_) {
      this.setSize(_size_);
      this.setConditional(_conditional_);
      this.setJumpToSubroutine(_jumpToSubroutine_);
      this.setReturn(_return_);
      this._subroutine_.clear();
      this._subroutine_.addAll(_subroutine_);
   }

   public AProgram(PSize _size_, PRsaddCommand _conditional_, PJumpToSubroutine _jumpToSubroutine_, PReturn _return_, XPSubroutine _subroutine_) {
      this.setSize(_size_);
      this.setConditional(_conditional_);
      this.setJumpToSubroutine(_jumpToSubroutine_);
      this.setReturn(_return_);
      if (_subroutine_ != null) {
         while (_subroutine_ instanceof X1PSubroutine) {
            this._subroutine_.addFirst(((X1PSubroutine)_subroutine_).getPSubroutine());
            _subroutine_ = ((X1PSubroutine)_subroutine_).getXPSubroutine();
         }

         this._subroutine_.addFirst(((X2PSubroutine)_subroutine_).getPSubroutine());
      }
   }

   @Override
   public Object clone() {
      return new AProgram(
         (PSize)this.cloneNode(this._size_),
         (PRsaddCommand)this.cloneNode(this._conditional_),
         (PJumpToSubroutine)this.cloneNode(this._jumpToSubroutine_),
         (PReturn)this.cloneNode(this._return_),
         this.cloneList(this._subroutine_)
      );
   }

   @Override
   public void apply(Switch sw) {
      ((Analysis)sw).caseAProgram(this);
   }

   public PSize getSize() {
      return this._size_;
   }

   public void setSize(PSize node) {
      if (this._size_ != null) {
         this._size_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._size_ = node;
   }

   public PRsaddCommand getConditional() {
      return this._conditional_;
   }

   public void setConditional(PRsaddCommand node) {
      if (this._conditional_ != null) {
         this._conditional_.parent(null);
      }

      if (node != null) {
         if (node.parent() != null) {
            node.parent().removeChild(node);
         }

         node.parent(this);
      }

      this._conditional_ = node;
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

   public LinkedList<PSubroutine> getSubroutine() {
      return this._subroutine_;
   }

   public void setSubroutine(List<? extends PSubroutine> list) {
      this._subroutine_.clear();
      this._subroutine_.addAll(list);
   }

   @Override
   public String toString() {
      return this.toString(this._size_)
         + this.toString(this._conditional_)
         + this.toString(this._jumpToSubroutine_)
         + this.toString(this._return_)
         + this.toString(this._subroutine_);
   }

   @Override
   void removeChild(Node child) {
      if (this._size_ == child) {
         this._size_ = null;
      } else if (this._conditional_ == child) {
         this._conditional_ = null;
      } else if (this._jumpToSubroutine_ == child) {
         this._jumpToSubroutine_ = null;
      } else if (this._return_ == child) {
         this._return_ = null;
      } else if (!this._subroutine_.remove(child)) {
         ;
      }
   }

   @Override
   void replaceChild(Node oldChild, Node newChild) {
      if (this._size_ == oldChild) {
         this.setSize((PSize)newChild);
      } else if (this._conditional_ == oldChild) {
         this.setConditional((PRsaddCommand)newChild);
      } else if (this._jumpToSubroutine_ == oldChild) {
         this.setJumpToSubroutine((PJumpToSubroutine)newChild);
      } else if (this._return_ == oldChild) {
         this.setReturn((PReturn)newChild);
      } else {
         ListIterator<PSubroutine> i = this._subroutine_.listIterator();

         while (i.hasNext()) {
            if (i.next() == oldChild) {
               if (newChild != null) {
                  i.set((PSubroutine)newChild);
                  oldChild.parent(null);
                  return;
               }

               i.remove();
               oldChild.parent(null);
               return;
            }
         }
      }
   }

   private class Subroutine_Cast implements Cast<PSubroutine> {
      private static final long serialVersionUID = 1L;
      Subroutine_Cast() {
      }

      @Override
      public PSubroutine cast(Object o) {
         PSubroutine node = (PSubroutine)o;
         if (node.parent() != null && node.parent() != AProgram.this) {
            node.parent().removeChild(node);
         }

         if (node.parent() == null || node.parent() != AProgram.this) {
            node.parent(AProgram.this);
         }

         return node;
      }
   }
}

