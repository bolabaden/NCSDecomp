// Copyright 2021-2025 NCSDecomp
// Licensed under the Business Source License 1.1 (BSL 1.1).
// Visit https://bolabaden.org for more information and other ventures
// See LICENSE.txt file in the project root for full license information.

package com.kotor.resource.formats.ncs.scriptutils;

import com.kotor.resource.formats.ncs.scriptnode.AActionExp;
import com.kotor.resource.formats.ncs.scriptnode.AConst;

/**
 * Heuristics for deriving readable variable names from common NWScript actions.
 */
public class NameGenerator {
   private static String actionParamTag(Object in) {
      if (AConst.class.isInstance(in)) {
         String str = ((AConst)in).toString();
         if (str.length() > 2) {
            return (Character.toUpperCase(str.charAt(1)) + str.substring(2, str.length() - 1)).replace(' ', '_');
         }
      }

      return null;
   }

   private static int actionParamToInt(Object in) {
      return AConst.class.isInstance(in) ? Integer.parseInt(((AConst)in).toString()) : -1;
   }

   public static String getNameFromAction(AActionExp actionexp) {
      String action = actionexp.action();
      if (action.equals("GetObjectByTag")) {
         String tag = actionParamTag(actionexp.getParam(0));
         return tag != null ? "o" + tag : null;
      } else if (action.equals("GetFirstPC")) {
         return "oPC";
      } else if (action.equals("GetScriptParameter")) {
         int i = actionParamToInt(actionexp.getParam(0));
         return i > 0 ? "nParam" + Integer.toString(i) : "nParam";
      } else if (action.equals("GetScriptStringParameter")) {
         return "sParam";
      } else if (action.equals("GetMaxHitPoints")) {
         return "nMaxHP";
      } else if (action.equals("GetCurrentHitPoints")) {
         return "nCurHP";
      } else if (action.equals("Random")) {
         return "nRandom";
      } else if (action.equals("GetArea")) {
         return "oArea";
      } else if (action.equals("GetEnteringObject")) {
         return "oEntering";
      } else if (action.equals("GetExitingObject")) {
         return "oExiting";
      } else if (action.equals("GetPosition")) {
         return "vPosition";
      } else if (action.equals("GetFacing")) {
         return "fFacing";
      } else if (action.equals("GetLastAttacker")) {
         return "oAttacker";
      } else if (action.equals("GetNearestCreature")) {
         return "oNearest";
      } else if (action.equals("GetDistanceToObject")) {
         return "fDistance";
      } else if (action.equals("GetIsObjectValid")) {
         return "nValid";
      } else if (action.equals("GetSpellTargetObject")) {
         return "oTarget";
      } else if (action.equals("EffectAssuredHit")) {
         return "efHit";
      } else if (action.equals("GetLastItemEquipped")) {
         return "oLastEquipped";
      } else if (action.equals("GetCurrentForcePoints")) {
         return "nCurFP";
      } else if (action.equals("GetMaxForcePoints")) {
         return "nMaxFP";
      } else if (action.equals("EffectHeal")) {
         return "efHeal";
      } else if (action.equals("EffectDamage")) {
         return "efDamage";
      } else if (action.equals("EffectAbilityIncrease")) {
         return "efAbilityInc";
      } else if (action.equals("EffectDamageResistance")) {
         return "efDamageRes";
      } else if (action.equals("EffectResurrection")) {
         return "efResurrect";
      } else if (action.equals("GetCasterLevel")) {
         return "nCasterLevel";
      } else if (action.equals("GetFirstObjectInArea")) {
         return "oAreaObject";
      } else if (action.equals("GetNextObjectInArea")) {
         return "oAreaObject";
      } else if (action.equals("GetObjectType")) {
         return "nType";
      } else if (action.equals("GetRacialType")) {
         return "nRace";
      } else if (action.equals("EffectACIncrease")) {
         return "efACInc";
      } else if (action.equals("EffectSavingThrowIncrease")) {
         return "efSaveInc";
      } else if (action.equals("EffectAttackIncrease")) {
         return "efAttackInc";
      } else if (action.equals("EffectDamageReduction")) {
         return "efDamageDec";
      } else if (action.equals("EffectDamageIncrease")) {
         return "efDamageInc";
      } else if (action.equals("GetGoodEvilValue")) {
         return "nAlign";
      } else if (action.equals("GetPartyMemberCount")) {
         return "nPartyCount";
      } else if (action.equals("GetAlignmentGoodEvil")) {
         return "nAlign";
      } else if (action.equals("GetFirstObjectInShape")) {
         return "oShapeObject";
      } else if (action.equals("GetNextObjectInShape")) {
         return "oShapeObject";
      } else if (action.equals("EffectEntangle")) {
         return "efEntangle";
      } else if (action.equals("EffectDeath")) {
         return "efDeath";
      } else if (action.equals("EffectKnockdown")) {
         return "efKnockdown";
      } else if (action.equals("GetAbilityScore")) {
         int i = actionParamToInt(actionexp.getParam(1));
         switch (i) {
            case 0:
               return "nStrength";
            case 1:
               return "nDex";
            case 2:
               return "nConst";
            case 3:
               return "nInt";
            case 4:
               return "nWis";
            case 5:
               return "nChar";
            default:
               return "nAbility";
         }
      } else if (action.equals("EffectParalyze")) {
         return "efParalyze";
      } else if (action.equals("EffectSpellImmunity")) {
         return "efSpellImm";
      } else if (action.equals("GetDistanceBetween")) {
         return "fDistance";
      } else if (action.equals("EffectForceJump")) {
         return "efForceJump";
      } else if (action.equals("EffectSleep")) {
         return "efSleep";
      } else if (action.equals("GetItemInSlot")) {
         int i = actionParamToInt(actionexp.getParam(0));
         switch (i) {
            case 0:
               return "oHeadItem";
            case 1:
               return "oBodyItem";
            case 2:
            case 6:
            case 11:
            case 12:
            case 13:
            default:
               return "oSlotItem";
            case 3:
               return "oHandsItem";
            case 4:
               return "oRWeapItem";
            case 5:
               return "oLWeapItem";
            case 7:
               return "oLArmItem";
            case 8:
               return "oRArmItem";
            case 9:
               return "oImplantItem";
            case 10:
               return "oBeltItem";
            case 14:
               return "oCWeapLItem";
            case 15:
               return "oCWeapRItem";
            case 16:
               return "oCWeapBItem";
            case 17:
               return "oCArmourItem";
            case 18:
               return "oRWeap2Item";
            case 19:
               return "oLWeap2Item";
         }
      } else if (action.equals("EffectTemporaryForcePoints")) {
         return "efTempFP";
      } else if (action.equals("EffectConfused")) {
         return "efConfused";
      } else if (action.equals("EffectFrightened")) {
         return "efFright";
      } else if (action.equals("EffectChoke")) {
         return "efChoke";
      } else if (action.equals("EffectStunned")) {
         return "efStun";
      } else if (action.equals("EffectRegenerate")) {
         return "efRegen";
      } else if (action.equals("EffectMovementSpeedIncrease")) {
         return "efSpeedInc";
      } else if (action.equals("GetHitDice")) {
         return "nLevel";
      } else if (action.equals("GetEffectType")) {
         return "nEfType";
      } else if (action.equals("EffectAreaOfEffect")) {
         return "efAOE";
      } else if (action.equals("EffectVisualEffect")) {
         return "efVisual";
      } else if (action.equals("GetFactionWeakestMember")) {
         return "oWeakest";
      } else if (action.equals("GetFactionStrongestMember")) {
         return "oStrongest";
      } else if (action.equals("GetFactionMostDamagedMember")) {
         return "oMostDamaged";
      } else if (action.equals("GetFactionLeastDamagedMember")) {
         return "oLeastDamaged";
      } else if (action.equals("GetWaypointByTag")) {
         String tag = actionParamTag(actionexp.getParam(0));
         return tag != null ? "o" + tag : "oWP";
      } else if (action.equals("GetTransitionTarget")) {
         return "oTransTarget";
      } else if (action.equals("EffectBeam")) {
         return "efBeam";
      } else if (action.equals("GetReputation")) {
         return "nRep";
      } else if (action.equals("GetModuleFileName")) {
         return "sModule";
      } else if (action.equals("EffectForceResistanceIncrease")) {
         return "efForceResInc";
      } else if (action.equals("GetSpellTargetLocation")) {
         return "locTarget";
      } else if (action.equals("EffectBodyFuel")) {
         return "efFuel";
      } else if (action.equals("GetFacingFromLocation")) {
         return "fFacing";
      } else if (action.equals("GetNearestCreatureToLocation")) {
         return "oNearestCreat";
      } else if (action.equals("GetNearestObject")) {
         return "oNearest";
      } else if (action.equals("GetNearestObjectToLocation")) {
         return "oNearest";
      } else if (action.equals("GetNearestObjectByTag")) {
         String tag = actionParamTag(actionexp.getParam(0));
         return tag != null ? "oNearest" + tag : null;
      } else if (action.equals("GetPCSpeaker")) {
         return "oSpeaker";
      } else if (action.equals("GetModule")) {
         return "oModule";
      } else if (action.equals("CreateObject")) {
         String tag = actionParamTag(actionexp.getParam(1));
         return tag != null ? "o" + tag : null;
      } else if (action.equals("EventSpellCastAt")) {
         return "evSpellCast";
      } else if (action.equals("GetLastSpellCaster")) {
         return "oCaster";
      } else if (action.equals("EffectPoison")) {
         return "efPoison";
      } else if (action.equals("EffectAssuredDeflection")) {
         return "efDeflect";
      } else if (action.equals("GetName")) {
         return "sName";
      } else if (action.equals("GetLastSpeaker")) {
         return "oSpeaker";
      } else if (action.equals("GetLastPerceived")) {
         return "oPerceived";
      } else if (action.equals("EffectForcePushTargeted")) {
         return "efPush";
      } else if (action.equals("EffectHaste")) {
         return "efHaste";
      } else if (action.equals("EffectImmunity")) {
         return "efImmunity";
      } else if (action.equals("GetIsImmune")) {
         return "nImmune";
      } else if (action.equals("EffectDamageImmunityIncrease")) {
         return "efDamageImmInc";
      } else if (action.equals("GetDistanceBetweenLocations")) {
         return "fDistance";
      } else if (action.equals("GetLocalNumber")) {
         return "nLocal";
      } else if (action.equals("GetStringLength")) {
         return "nLen";
      } else if (action.equals("GetObjectPersonalSpace")) {
         return "fPersonalSpace";
      } else if (action.equals("d8")) {
         return "nRandom";
      } else if (action.equals("d10")) {
         return "nRandom";
      } else if (action.equals("GetPartyMemberByIndex")) {
         return "oNPC";
      } else if (action.equals("GetAttackTarget")) {
         return "oTarget";
      } else if (action.equals("GetCreatureTalentRandom")) {
         return "talRandom";
      } else if (action.equals("GetPUPOwner")) {
         return "oPUPOwner";
      } else if (action.equals("GetDistanceToObject2D")) {
         return "fDistance";
      } else if (action.equals("GetCurrentAction")) {
         return "nAction";
      } else if (action.equals("GetPartyLeader")) {
         return "oLeader";
      } else if (action.equals("GetFirstEffect")) {
         return "efFirst";
      } else if (action.equals("GetNextEffect")) {
         return "efNext";
      } else if (action.equals("GetPartyAIStyle")) {
         return "nStyle";
      } else if (action.equals("GetNPCAIStyle")) {
         return "nNPCStyle";
      } else if (action.equals("GetLastHostileTarget")) {
         return "oLastTarget";
      } else if (action.equals("GetLastHostileActor")) {
         return "oLastActor";
      } else if (action.equals("GetRandomDestination")) {
         return "vRandom";
      } else if (action.equals("Location")) {
         return null;
      } else if (action.equals("GetHealTarget")) {
         return "oTarget";
      } else if (action.equals("GetCreatureTalentBest")) {
         return "talBest";
      } else if (action.equals("d4")) {
         return "nRandom";
      } else if (action.equals("d6")) {
         return "nRandom";
      } else if (action.equals("d100")) {
         return "nRandom";
      } else if (action.equals("d3")) {
         return "nRandom";
      } else if (action.equals("GetIdFromTalent")) {
         return "nTalent";
      } else if (action.equals("GetLocalBoolean")) {
         return "nLocalBool";
      } else if (action.equals("TalentSpell")) {
         return "talSpell";
      } else if (action.equals("TalentFeat")) {
         return "talFeat";
      } else if (action.equals("FloatToString")) {
         return null;
      } else if (action.equals("GetLocation")) {
         return null;
      } else if (action.equals("IntToString")) {
         return null;
      } else if (action.equals("GetGlobalNumber")) {
         return "nGlobal";
      } else if (action.equals("GetBaseItemType")) {
         return "nItemType";
      } else if (action.equals("GetFirstItemInInventory")) {
         return "oInvItem";
      } else if (action.equals("GetNextItemInInventory")) {
         return "oInvItem";
      } else if (action.equals("GetSpellBaseForcePointCost")) {
         return "nBaseFP";
      } else if (action.equals("GetLastForcePowerUsed")) {
         return "nLastForce";
      } else if (action.equals("StringToInt")) {
         return null;
      } else {
         System.out.println("Variable Naming: consider adding " + action);
         return null;
      }
   }
}

