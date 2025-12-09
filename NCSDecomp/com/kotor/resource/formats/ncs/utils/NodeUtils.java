// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.utils;

import com.kotor.resource.formats.ncs.ActionsData;
import com.kotor.resource.formats.ncs.node.AActionCmd;
import com.kotor.resource.formats.ncs.node.AActionCommand;
import com.kotor.resource.formats.ncs.node.AActionJumpCmd;
import com.kotor.resource.formats.ncs.node.AAddBinaryOp;
import com.kotor.resource.formats.ncs.node.AAddVarCmd;
import com.kotor.resource.formats.ncs.node.AAndLogiiOp;
import com.kotor.resource.formats.ncs.node.ABinaryCmd;
import com.kotor.resource.formats.ncs.node.ABinaryCommand;
import com.kotor.resource.formats.ncs.node.ABitAndLogiiOp;
import com.kotor.resource.formats.ncs.node.ABpCmd;
import com.kotor.resource.formats.ncs.node.ABpCommand;
import com.kotor.resource.formats.ncs.node.ACommandBlock;
import com.kotor.resource.formats.ncs.node.ACompUnaryOp;
import com.kotor.resource.formats.ncs.node.ACondJumpCmd;
import com.kotor.resource.formats.ncs.node.AConditionalJumpCommand;
import com.kotor.resource.formats.ncs.node.AConstCmd;
import com.kotor.resource.formats.ncs.node.AConstCommand;
import com.kotor.resource.formats.ncs.node.ACopyDownBpCommand;
import com.kotor.resource.formats.ncs.node.ACopyDownSpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopBpCommand;
import com.kotor.resource.formats.ncs.node.ACopyTopSpCommand;
import com.kotor.resource.formats.ncs.node.ACopydownbpCmd;
import com.kotor.resource.formats.ncs.node.ACopydownspCmd;
import com.kotor.resource.formats.ncs.node.ACopytopbpCmd;
import com.kotor.resource.formats.ncs.node.ACopytopspCmd;
import com.kotor.resource.formats.ncs.node.ADecibpStackOp;
import com.kotor.resource.formats.ncs.node.ADecispStackOp;
import com.kotor.resource.formats.ncs.node.ADestructCmd;
import com.kotor.resource.formats.ncs.node.ADestructCommand;
import com.kotor.resource.formats.ncs.node.ADivBinaryOp;
import com.kotor.resource.formats.ncs.node.AEqualBinaryOp;
import com.kotor.resource.formats.ncs.node.AExclOrLogiiOp;
import com.kotor.resource.formats.ncs.node.AFloatConstant;
import com.kotor.resource.formats.ncs.node.AGeqBinaryOp;
import com.kotor.resource.formats.ncs.node.AGtBinaryOp;
import com.kotor.resource.formats.ncs.node.AIncibpStackOp;
import com.kotor.resource.formats.ncs.node.AIncispStackOp;
import com.kotor.resource.formats.ncs.node.AInclOrLogiiOp;
import com.kotor.resource.formats.ncs.node.AIntConstant;
import com.kotor.resource.formats.ncs.node.AJumpCmd;
import com.kotor.resource.formats.ncs.node.AJumpCommand;
import com.kotor.resource.formats.ncs.node.AJumpSubCmd;
import com.kotor.resource.formats.ncs.node.AJumpToSubroutine;
import com.kotor.resource.formats.ncs.node.ALeqBinaryOp;
import com.kotor.resource.formats.ncs.node.ALogiiCmd;
import com.kotor.resource.formats.ncs.node.ALogiiCommand;
import com.kotor.resource.formats.ncs.node.ALtBinaryOp;
import com.kotor.resource.formats.ncs.node.AModBinaryOp;
import com.kotor.resource.formats.ncs.node.AMoveSpCommand;
import com.kotor.resource.formats.ncs.node.AMovespCmd;
import com.kotor.resource.formats.ncs.node.AMulBinaryOp;
import com.kotor.resource.formats.ncs.node.ANegUnaryOp;
import com.kotor.resource.formats.ncs.node.ANequalBinaryOp;
import com.kotor.resource.formats.ncs.node.ANotUnaryOp;
import com.kotor.resource.formats.ncs.node.AOrLogiiOp;
import com.kotor.resource.formats.ncs.node.AProgram;
import com.kotor.resource.formats.ncs.node.AReturn;
import com.kotor.resource.formats.ncs.node.AReturnCmd;
import com.kotor.resource.formats.ncs.node.ARsaddCommand;
import com.kotor.resource.formats.ncs.node.AShleftBinaryOp;
import com.kotor.resource.formats.ncs.node.AShrightBinaryOp;
import com.kotor.resource.formats.ncs.node.AStackCommand;
import com.kotor.resource.formats.ncs.node.AStackOpCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCmd;
import com.kotor.resource.formats.ncs.node.AStoreStateCommand;
import com.kotor.resource.formats.ncs.node.AStringConstant;
import com.kotor.resource.formats.ncs.node.ASubBinaryOp;
import com.kotor.resource.formats.ncs.node.ASubroutine;
import com.kotor.resource.formats.ncs.node.AUnaryCmd;
import com.kotor.resource.formats.ncs.node.AUnaryCommand;
import com.kotor.resource.formats.ncs.node.AUnrightBinaryOp;
import com.kotor.resource.formats.ncs.node.AZeroJumpIf;
import com.kotor.resource.formats.ncs.node.Node;
import com.kotor.resource.formats.ncs.node.PBinaryOp;
import com.kotor.resource.formats.ncs.node.PConstant;
import com.kotor.resource.formats.ncs.node.PLogiiOp;
import com.kotor.resource.formats.ncs.node.PStackOp;
import com.kotor.resource.formats.ncs.node.PUnaryOp;
import com.kotor.resource.formats.ncs.node.Start;
import com.kotor.resource.formats.ncs.node.PCmd;
import com.kotor.resource.formats.ncs.node.TIntegerConstant;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Static helpers for inspecting/deriving properties of SableCC AST nodes.
 */
public final class NodeUtils {
   public static final int CMDSIZE_JUMP = 6;
   public static final int CMDSIZE_RETN = 2;

   public static boolean isStoreStackNode(Node node) {
      if (ALogiiCmd.class.isInstance(node)) {
         ALogiiCommand lnode = (ALogiiCommand)((ALogiiCmd)node).getLogiiCommand();
         if (AOrLogiiOp.class.isInstance(lnode.getLogiiOp())) {
            return false;
         }
      }

      return true;
   }

   public static boolean isJzPastOne(Node node) {
      return AConditionalJumpCommand.class.isInstance(node) && AZeroJumpIf.class.isInstance(((AConditionalJumpCommand)node).getJumpIf())
         ? Integer.parseInt(((AConditionalJumpCommand)node).getOffset().getText()) == 12
         : false;
   }

   public static boolean isJz(Node node) {
      return AConditionalJumpCommand.class.isInstance(node) ? AZeroJumpIf.class.isInstance(((AConditionalJumpCommand)node).getJumpIf()) : false;
   }

   public static boolean isCommandNode(Node node) {
      return AConditionalJumpCommand.class.isInstance(node)
         || AJumpCommand.class.isInstance(node)
         || AJumpToSubroutine.class.isInstance(node)
         || AReturn.class.isInstance(node)
         || ACopyDownSpCommand.class.isInstance(node)
         || ACopyTopSpCommand.class.isInstance(node)
         || ACopyDownBpCommand.class.isInstance(node)
         || ACopyTopBpCommand.class.isInstance(node)
         || AMoveSpCommand.class.isInstance(node)
         || ARsaddCommand.class.isInstance(node)
         || AConstCommand.class.isInstance(node)
         || AActionCommand.class.isInstance(node)
         || ALogiiCommand.class.isInstance(node)
         || ABinaryCommand.class.isInstance(node)
         || AUnaryCommand.class.isInstance(node)
         || AStackCommand.class.isInstance(node)
         || ADestructCommand.class.isInstance(node)
         || ABpCommand.class.isInstance(node)
         || AStoreStateCommand.class.isInstance(node);
   }

   public static int getCommandPos(Node node) {
      if (AConditionalJumpCommand.class.isInstance(node)) {
         return Integer.parseInt(((AConditionalJumpCommand)node).getPos().getText());
      } else if (AJumpCommand.class.isInstance(node)) {
         return Integer.parseInt(((AJumpCommand)node).getPos().getText());
      } else if (AJumpToSubroutine.class.isInstance(node)) {
         return Integer.parseInt(((AJumpToSubroutine)node).getPos().getText());
      } else if (AReturn.class.isInstance(node)) {
         return Integer.parseInt(((AReturn)node).getPos().getText());
      } else if (ACopyDownSpCommand.class.isInstance(node)) {
         return Integer.parseInt(((ACopyDownSpCommand)node).getPos().getText());
      } else if (ACopyTopSpCommand.class.isInstance(node)) {
         return Integer.parseInt(((ACopyTopSpCommand)node).getPos().getText());
      } else if (ACopyDownBpCommand.class.isInstance(node)) {
         return Integer.parseInt(((ACopyDownBpCommand)node).getPos().getText());
      } else if (ACopyTopBpCommand.class.isInstance(node)) {
         return Integer.parseInt(((ACopyTopBpCommand)node).getPos().getText());
      } else if (AMoveSpCommand.class.isInstance(node)) {
         return Integer.parseInt(((AMoveSpCommand)node).getPos().getText());
      } else if (ARsaddCommand.class.isInstance(node)) {
         return Integer.parseInt(((ARsaddCommand)node).getPos().getText());
      } else if (AConstCommand.class.isInstance(node)) {
         return Integer.parseInt(((AConstCommand)node).getPos().getText());
      } else if (AActionCommand.class.isInstance(node)) {
         return Integer.parseInt(((AActionCommand)node).getPos().getText());
      } else if (ALogiiCommand.class.isInstance(node)) {
         return Integer.parseInt(((ALogiiCommand)node).getPos().getText());
      } else if (ABinaryCommand.class.isInstance(node)) {
         return Integer.parseInt(((ABinaryCommand)node).getPos().getText());
      } else if (AUnaryCommand.class.isInstance(node)) {
         return Integer.parseInt(((AUnaryCommand)node).getPos().getText());
      } else if (AStackCommand.class.isInstance(node)) {
         return Integer.parseInt(((AStackCommand)node).getPos().getText());
      } else if (ADestructCommand.class.isInstance(node)) {
         return Integer.parseInt(((ADestructCommand)node).getPos().getText());
      } else if (ABpCommand.class.isInstance(node)) {
         return Integer.parseInt(((ABpCommand)node).getPos().getText());
      } else {
         return AStoreStateCommand.class.isInstance(node) ? Integer.parseInt(((AStoreStateCommand)node).getPos().getText()) : -1;
      }
   }

   public static int getJumpDestinationPos(Node node) {
      if (AConditionalJumpCommand.class.isInstance(node)) {
         return Integer.parseInt(((AConditionalJumpCommand)node).getPos().getText()) + Integer.parseInt(((AConditionalJumpCommand)node).getOffset().getText());
      } else if (AJumpCommand.class.isInstance(node)) {
         return Integer.parseInt(((AJumpCommand)node).getPos().getText()) + Integer.parseInt(((AJumpCommand)node).getOffset().getText());
      } else {
         return AJumpToSubroutine.class.isInstance(node)
            ? Integer.parseInt(((AJumpToSubroutine)node).getPos().getText()) + Integer.parseInt(((AJumpToSubroutine)node).getOffset().getText())
            : -1;
      }
   }

   public static boolean isEqualityOp(ABinaryCommand node) {
      PBinaryOp op = node.getBinaryOp();
      return AEqualBinaryOp.class.isInstance(op) || ANequalBinaryOp.class.isInstance(op);
   }

   public static boolean isConditionalOp(ABinaryCommand node) {
      PBinaryOp op = node.getBinaryOp();
      return AEqualBinaryOp.class.isInstance(op)
         || ANequalBinaryOp.class.isInstance(op)
         || ALtBinaryOp.class.isInstance(op)
         || ALeqBinaryOp.class.isInstance(op)
         || AGtBinaryOp.class.isInstance(op)
         || AGeqBinaryOp.class.isInstance(op);
   }

   public static boolean isArithmeticOp(ABinaryCommand node) {
      PBinaryOp op = node.getBinaryOp();
      return AAddBinaryOp.class.isInstance(op)
         || ASubBinaryOp.class.isInstance(op)
         || ADivBinaryOp.class.isInstance(op)
         || AMulBinaryOp.class.isInstance(op)
         || AModBinaryOp.class.isInstance(op)
         || AShleftBinaryOp.class.isInstance(op)
         || AShrightBinaryOp.class.isInstance(op)
         || AUnrightBinaryOp.class.isInstance(op);
   }

   public static boolean isVectorAllowedOp(ABinaryCommand node) {
      PBinaryOp op = node.getBinaryOp();
      return AAddBinaryOp.class.isInstance(op) || ASubBinaryOp.class.isInstance(op) || ADivBinaryOp.class.isInstance(op) || AMulBinaryOp.class.isInstance(op);
   }

   public static String getOp(AUnaryCommand node) {
      PUnaryOp op = node.getUnaryOp();
      if (ANegUnaryOp.class.isInstance(op)) {
         return "-";
      } else if (ACompUnaryOp.class.isInstance(op)) {
         return "~";
      } else if (ANotUnaryOp.class.isInstance(op)) {
         return "!";
      } else {
         throw new RuntimeException("unknown unary op");
      }
   }

   public static String getOp(ABinaryCommand node) {
      PBinaryOp op = node.getBinaryOp();
      if (AAddBinaryOp.class.isInstance(op)) {
         return "+";
      } else if (ASubBinaryOp.class.isInstance(op)) {
         return "-";
      } else if (ADivBinaryOp.class.isInstance(op)) {
         return "/";
      } else if (AMulBinaryOp.class.isInstance(op)) {
         return "*";
      } else if (AModBinaryOp.class.isInstance(op)) {
         return "%";
      } else if (AShleftBinaryOp.class.isInstance(op)) {
         return "<<";
      } else if (AShrightBinaryOp.class.isInstance(op)) {
         return ">>";
      } else if (AUnrightBinaryOp.class.isInstance(op)) {
         throw new RuntimeException("found an unsigned bit shift.");
      } else if (AEqualBinaryOp.class.isInstance(op)) {
         return "==";
      } else if (ANequalBinaryOp.class.isInstance(op)) {
         return "!=";
      } else if (ALtBinaryOp.class.isInstance(op)) {
         return "<";
      } else if (ALeqBinaryOp.class.isInstance(op)) {
         return "<=";
      } else if (AGtBinaryOp.class.isInstance(op)) {
         return ">";
      } else if (AGeqBinaryOp.class.isInstance(op)) {
         return ">=";
      } else {
         throw new RuntimeException("unknown binary op");
      }
   }

   public static String getOp(ALogiiCommand node) {
      PLogiiOp op = node.getLogiiOp();
      if (AAndLogiiOp.class.isInstance(op)) {
         return "&&";
      } else if (AOrLogiiOp.class.isInstance(op)) {
         return "||";
      } else if (AInclOrLogiiOp.class.isInstance(op)) {
         return "|";
      } else if (AExclOrLogiiOp.class.isInstance(op)) {
         return "^";
      } else if (ABitAndLogiiOp.class.isInstance(op)) {
         return "&";
      } else {
         throw new RuntimeException("unknown logii op");
      }
   }

   public static String getOp(AStackCommand node) {
      PStackOp op = node.getStackOp();
      if (ADecispStackOp.class.isInstance(op) || ADecibpStackOp.class.isInstance(op)) {
         return "--";
      } else if (!AIncispStackOp.class.isInstance(op) && !AIncibpStackOp.class.isInstance(op)) {
         throw new RuntimeException("unknown relative-to-stack unary modifier op");
      } else {
         return "++";
      }
   }

   public static boolean isGlobalStackOp(AStackCommand node) {
      PStackOp op = node.getStackOp();
      return AIncibpStackOp.class.isInstance(op) || ADecibpStackOp.class.isInstance(op);
   }

   public static int getParam1Size(ABinaryCommand node) {
      Type type = getType(node);
      return !type.equals((byte)59) && !type.equals((byte)58) ? 1 : 3;
   }

   public static int getParam2Size(ABinaryCommand node) {
      Type type = getType(node);
      return !type.equals((byte)60) && !type.equals((byte)58) ? 1 : 3;
   }

   public static int getResultSize(ABinaryCommand node) {
      Type type = getType(node);
      return !type.equals((byte)60) && !type.equals((byte)59) && !type.equals((byte)58) ? 1 : 3;
   }

   public static Object getConstValue(AConstCommand node) {
      PConstant pconst = node.getConstant();
      Type type = getType(node);
      switch (type.byteValue()) {
         case 3:
            return Long.parseLong(((AIntConstant)pconst).getIntegerConstant().getText());
         case 4:
            return Float.parseFloat(((AFloatConstant)pconst).getFloatConstant().getText());
         case 5:
            return ((AStringConstant)pconst).getStringLiteral().getText();
         case 6:
            return Integer.parseInt(((AIntConstant)pconst).getIntegerConstant().getText());
         default:
            throw new RuntimeException("Invalid const type " + type);
      }
   }

   public static int getSubEnd(ASubroutine sub) {
      return getCommandPos(sub.getReturn());
   }

   public static int getActionId(AActionCommand node) {
      return Integer.parseInt(node.getId().getText());
   }

   public static int getActionParamCount(AActionCommand node) {
      return Integer.parseInt(node.getArgCount().getText());
   }

   public static String getActionName(AActionCommand node, ActionsData actions) {
      return actions.getName(getActionId(node));
   }

   public static List<Type> getActionParamTypes(AActionCommand node, ActionsData actions) {
      if (actions == null) {
         throw new RuntimeException("ActionsData is null when trying to get param types for action ID: " + getActionId(node));
      }
      int actionId = getActionId(node);
      return actions.getParamTypes(actionId);
   }

   public static int actionRemoveElementCount(AActionCommand node, ActionsData actions) {
      List<Type> types = getActionParamTypes(node, actions);
      int count = getActionParamCount(node);
      int remove = 0;

      for (int i = 0; i < count; i++) {
         remove += types.get(i).typeSize();
      }

      return stackSizeToPos(remove);
   }

   public static int stackOffsetToPos(TIntegerConstant offset) {
      return -Integer.parseInt(offset.getText()) / 4;
   }

   public static int stackSizeToPos(TIntegerConstant offset) {
      return Integer.parseInt(offset.getText()) / 4;
   }

   public static int stackSizeToPos(int offset) {
      return offset / 4;
   }

   public static Node getCommandChild(Node node) {
      if (isCommandNode(node)) {
         return node;
      } else if (ASubroutine.class.isInstance(node)) {
         return getCommandChild(((ASubroutine)node).getCommandBlock());
      } else if (ACommandBlock.class.isInstance(node)) {
         return getCommandChild((Node)((ACommandBlock)node).getCmd().get(0));
      } else if (AAddVarCmd.class.isInstance(node)) {
         return getCommandChild(((AAddVarCmd)node).getRsaddCommand());
      } else if (AActionJumpCmd.class.isInstance(node)) {
         return getCommandChild(((AActionJumpCmd)node).getStoreStateCommand());
      } else if (AConstCmd.class.isInstance(node)) {
         return getCommandChild(((AConstCmd)node).getConstCommand());
      } else if (ACopydownspCmd.class.isInstance(node)) {
         return getCommandChild(((ACopydownspCmd)node).getCopyDownSpCommand());
      } else if (ACopytopspCmd.class.isInstance(node)) {
         return getCommandChild(((ACopytopspCmd)node).getCopyTopSpCommand());
      } else if (ACopydownbpCmd.class.isInstance(node)) {
         return getCommandChild(((ACopydownbpCmd)node).getCopyDownBpCommand());
      } else if (ACopytopbpCmd.class.isInstance(node)) {
         return getCommandChild(((ACopytopbpCmd)node).getCopyTopBpCommand());
      } else if (ACondJumpCmd.class.isInstance(node)) {
         return getCommandChild(((ACondJumpCmd)node).getConditionalJumpCommand());
      } else if (AJumpCmd.class.isInstance(node)) {
         return getCommandChild(((AJumpCmd)node).getJumpCommand());
      } else if (AJumpSubCmd.class.isInstance(node)) {
         return getCommandChild(((AJumpSubCmd)node).getJumpToSubroutine());
      } else if (AMovespCmd.class.isInstance(node)) {
         return getCommandChild(((AMovespCmd)node).getMoveSpCommand());
      } else if (ALogiiCmd.class.isInstance(node)) {
         return getCommandChild(((ALogiiCmd)node).getLogiiCommand());
      } else if (AUnaryCmd.class.isInstance(node)) {
         return getCommandChild(((AUnaryCmd)node).getUnaryCommand());
      } else if (ABinaryCmd.class.isInstance(node)) {
         return getCommandChild(((ABinaryCmd)node).getBinaryCommand());
      } else if (ADestructCmd.class.isInstance(node)) {
         return getCommandChild(((ADestructCmd)node).getDestructCommand());
      } else if (ABpCmd.class.isInstance(node)) {
         return getCommandChild(((ABpCmd)node).getBpCommand());
      } else if (AActionCmd.class.isInstance(node)) {
         return getCommandChild(((AActionCmd)node).getActionCommand());
      } else if (AStackOpCmd.class.isInstance(node)) {
         return getCommandChild(((AStackOpCmd)node).getStackCommand());
      } else if (AReturnCmd.class.isInstance(node)) {
         return getCommandChild(((AReturnCmd)node).getReturn());
      } else if (AStoreStateCmd.class.isInstance(node)) {
         return getCommandChild(((AStoreStateCmd)node).getStoreStateCommand());
      } else {
         throw new RuntimeException("unexpected node type " + node);
      }
   }

   public static Node getPreviousCommand(Node node, NodeAnalysisData nodedata) {
      if (AReturn.class.isInstance(node)) {
         System.out.println("class " + node.parent().getClass());
         ACommandBlock ablock = (ACommandBlock)((ASubroutine)node.parent()).getCommandBlock();
         return getCommandChild((Node)ablock.getCmd().getLast());
      } else {
         Node up = node.parent();

         while (!ACommandBlock.class.isInstance(up) && up != null) {
            up = up.parent();
         }

         if (up == null) {
            return null;
         } else {
            int searchPos = nodedata.getPos(node);
            ListIterator<PCmd> it = ((ACommandBlock)up).getCmd().listIterator();

            while (it.hasNext()) {
               PCmd cmd = it.next();
               if (nodedata.getPos(cmd) == searchPos) {
                  it.previous();
                  return getCommandChild(it.previous());
               }
            }

            return null;
         }
      }
   }

   public static Node getNextCommand(Node node, NodeAnalysisData nodedata) {
      Node up = node.parent();

      while (!ACommandBlock.class.isInstance(up)) {
         up = up.parent();
      }

      int searchPos = nodedata.getPos(node);
      Iterator<PCmd> it = ((ACommandBlock)up).getCmd().iterator();

      while (it.hasNext()) {
         PCmd next = it.next();
         if (nodedata.getPos(next) == searchPos) {
            if (it.hasNext()) {
               return getCommandChild(it.next());
            }

            return null;
         }
      }

      return null;
   }

   public static boolean isReturn(Node node) {
      return AReturnCmd.class.isInstance(node) || AReturn.class.isInstance(node);
   }

   public static Type getType(Node node) {
      if (AConditionalJumpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AConditionalJumpCommand)node).getType().getText()));
      } else if (AJumpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AJumpCommand)node).getType().getText()));
      } else if (AJumpToSubroutine.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AJumpToSubroutine)node).getType().getText()));
      } else if (AReturn.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AReturn)node).getType().getText()));
      } else if (ACopyDownSpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ACopyDownSpCommand)node).getType().getText()));
      } else if (ACopyTopSpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ACopyTopSpCommand)node).getType().getText()));
      } else if (ACopyDownBpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ACopyDownBpCommand)node).getType().getText()));
      } else if (ACopyTopBpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ACopyTopBpCommand)node).getType().getText()));
      } else if (AMoveSpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AMoveSpCommand)node).getType().getText()));
      } else if (ARsaddCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ARsaddCommand)node).getType().getText()));
      } else if (AConstCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AConstCommand)node).getType().getText()));
      } else if (AActionCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AActionCommand)node).getType().getText()));
      } else if (ALogiiCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ALogiiCommand)node).getType().getText()));
      } else if (ABinaryCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ABinaryCommand)node).getType().getText()));
      } else if (AUnaryCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AUnaryCommand)node).getType().getText()));
      } else if (AStackCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((AStackCommand)node).getType().getText()));
      } else if (ADestructCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ADestructCommand)node).getType().getText()));
      } else if (ABpCommand.class.isInstance(node)) {
         return new Type(Byte.parseByte(((ABpCommand)node).getType().getText()));
      } else {
         throw new RuntimeException("No type for this node type: " + node);
      }
   }

   public static Type getReturnType(AActionCommand node, ActionsData actions) {
      return actions.getReturnType(getActionId(node));
   }

   public static Type getReturnType(ABinaryCommand node) {
      byte nodetype = Byte.parseByte(node.getType().getText());
      byte type;
      if (nodetype == 60 || nodetype == 59 || nodetype == 58) {
         type = -16;
      } else if (nodetype == 32) {
         type = 3;
      } else if (nodetype != 37 && nodetype != 38 && nodetype != 33) {
         if (nodetype != 35) {
            throw new RuntimeException("Unexpected type " + Byte.toString(nodetype));
         }

         type = 5;
      } else {
         type = 4;
      }

      if (type == -16) {
         type = 4;
      }

      return new Type(type);
   }

   public static boolean isConditionalProgram(Start ast) {
      return ((AProgram)ast.getPProgram()).getConditional() != null;
   }
}

