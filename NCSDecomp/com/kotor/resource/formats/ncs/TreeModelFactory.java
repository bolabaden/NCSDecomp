// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs;

import java.util.Hashtable;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;

/**
 * Small helper for producing Swing {@link TreeModel} instances used in the GUI.
 */
public class TreeModelFactory extends JTree {
   private static final long serialVersionUID = 1L;
   protected static TreeModel emptyModel;

   public TreeModelFactory() {
      emptyModel = JTree.createTreeModel(new Hashtable<>());
   }

   /**
    * Builds a {@link TreeModel} from the provided root object.
    */
   public static TreeModel createTreeModel(Object object) {
      return JTree.createTreeModel(object);
   }

   /**
    * Returns an empty model that can be reused when no file is selected.
    */
   public static TreeModel getEmptyModel() {
      return emptyModel;
   }
}

