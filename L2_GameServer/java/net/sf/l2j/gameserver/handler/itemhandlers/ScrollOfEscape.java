/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.3.2.5 $ $Date: 2005/03/27 15:30:07 $
 */

public class ScrollOfEscape implements IItemHandler
{
	// all the items ids that this handler knowns
	private static final int[] ITEM_IDS =
	{
		736, 1830, 1829, 1538, 3958, 5858,
		5859, 7117, 7118, 7119, 7120, 7121,
		7122, 7123, 7124, 7125, 7126, 7127,
		7128, 7129, 7130, 7131, 7132, 7133,
		7134, 7135, 7554, 7555, 7556, 7557,
		7558, 7559, 7618, 7619, 9716, 10129,
		10130
	};
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
	 */
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;
		
		// Thanks nbd
		if (!TvTEvent.onEscapeUse(activeChar.getObjectId()))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isAlikeDead() || activeChar.isAllSkillsDisabled())
			return;
		
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return;
		}
		
		if (GrandBossManager.getInstance().getZone(activeChar) != null && !activeChar.isGM())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			//activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_SCROLL_OF_ESCAPE_INSIDE_BOSS_ZONE));
			
			// SystemMessage doesn't exist ??!
			activeChar.sendMessage("Cant summon target inside boss zone.");
			return;
		}
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		
		// Check to see if the player is in a festival.
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You may not use an escape skill in a festival.");
			return;
		}
		
		// Check to see if player is in jail
		if (activeChar.isInJail())
		{
			activeChar.sendMessage("You can not escape from jail.");
			return;
		}
		// Check to see if player is in a duel
		if (activeChar.isInDuel())
		{
			activeChar.sendMessage("You cannot use escape skills during a duel.");
			return;
		}
		
		//activeChar.abortCast();
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		//SoE Animation section
		activeChar.setTarget(activeChar);
		
		// Modified by Tempy - 28 Jul 05 \\
		// Check if this is a blessed scroll, if it is then shorten the cast time.
		int itemId = item.getItemId();
		int escapeSkill = (itemId == 1538 || itemId == 5858 || itemId == 5859 || itemId == 3958 || itemId == 10130) ? 2036 : 2013;
		
		if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
			return;
		
		activeChar.disableAllSkills();
		
		L2Skill skill = SkillTable.getInstance().getInfo(escapeSkill, 1);
		MagicSkillUse msu = new MagicSkillUse(activeChar, escapeSkill, 1, skill.getHitTime(), 0);
		activeChar.broadcastPacket(msu);
		SetupGauge sg = new SetupGauge(0, skill.getHitTime());
		activeChar.sendPacket(sg);
		//End SoE Animation section
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(item);
		activeChar.sendPacket(sm);
		
		EscapeFinalizer ef = new EscapeFinalizer(activeChar, itemId);
		// continue execution later
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(ef, skill.getHitTime()));
		activeChar.setSkillCastEndTime(10 + GameTimeController.getGameTicks() + skill.getHitTime() / GameTimeController.MILLIS_IN_TICK);
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		private int _itemId;
		
		EscapeFinalizer(L2PcInstance activeChar, int itemId)
		{
			_activeChar = activeChar;
			_itemId = itemId;
		}
		
		public void run()
		{
			if (_activeChar.isDead())
				return;
			_activeChar.enableAllSkills();
			
			_activeChar.setIsIn7sDungeon(false);
			
			try
			{
				if ((_itemId == 1830 || _itemId == 5859) && CastleManager.getInstance().getCastleByOwner(_activeChar.getClan()) != null) // escape to castle if own's one
				{
					_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Castle);
				}
				else if ((_itemId == 10129 || _itemId == 10130) && FortManager.getInstance().getFortByOwner(_activeChar.getClan()) != null) // escape to fortress if own's one
				{
					_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Fortress);
				}
				else if ((_itemId == 1829 || _itemId == 5858) && _activeChar.getClan() != null && ClanHallManager.getInstance().getClanHallByOwner(_activeChar.getClan()) != null) // escape to clan hall if own's one
				{
					_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.ClanHall);
				}
				else if (_itemId == 5858) // do nothing
				{
					//_activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_HAS_NO_CLAN_HALL));
					_activeChar.sendMessage("Your clan does not own a clanhall.");
					return;
				}
				else if (_itemId == 5859) // do nothing
				{
					_activeChar.sendMessage("Your clan does not own a castle.");
					return;
				}
				else if (_itemId == 10130) // do nothing
				{
					_activeChar.sendMessage("Your clan does not own a fortress.");
					return;
				}
				else
				{
					if (_itemId < 7117)
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					else
					{
						switch (_itemId)
						{
							case 7117:
								_activeChar.teleToLocation(-84318, 244579, -3730, true); // Talking Island
								break;
							case 7554:
								_activeChar.teleToLocation(-84318, 244579, -3730, true); // Talking Island quest scroll
								break;
							case 7118:
								_activeChar.teleToLocation(46934, 51467, -2977, true); // Elven Village
								break;
							case 7555:
								_activeChar.teleToLocation(46934, 51467, -2977, true); // Elven Village quest scroll
								break;
							case 7119:
								_activeChar.teleToLocation(9745, 15606, -4574, true); // Dark Elven Village
								break;
							case 7556:
								_activeChar.teleToLocation(9745, 15606, -4574, true); // Dark Elven Village quest scroll
								break;
							case 7120:
								_activeChar.teleToLocation(-44836, -112524, -235, true); // Orc Village
								break;
							case 7557:
								_activeChar.teleToLocation(-44836, -112524, -235, true); // Orc Village quest scroll
								break;
							case 7121:
								_activeChar.teleToLocation(115113, -178212, -901, true); // Dwarven Village
								break;
							case 7558:
								_activeChar.teleToLocation(115113, -178212, -901, true); // Dwarven Village quest scroll
								break;
							case 7122:
								_activeChar.teleToLocation(-80826, 149775, -3043, true); // Gludin Village
								break;
							case 7123:
								_activeChar.teleToLocation(-12678, 122776, -3116, true); // Gludio Castle Town
								break;
							case 7124:
								_activeChar.teleToLocation(15670, 142983, -2705, true); // Dion Castle Town
								break;
							case 7125:
								_activeChar.teleToLocation(17836, 170178, -3507, true); // Floran
								break;
							case 7126:
								_activeChar.teleToLocation(83400, 147943, -3404, true); // Giran Castle Town
								break;
							case 7559:
								_activeChar.teleToLocation(83400, 147943, -3404, true); // Giran Castle Town quest scroll
								break;
							case 7127:
								_activeChar.teleToLocation(105918, 109759, -3207, true); // Hardin's Private Academy
								break;
							case 7128:
								_activeChar.teleToLocation(111409, 219364, -3545, true); // Heine
								break;
							case 7129:
								_activeChar.teleToLocation(82956, 53162, -1495, true); // Oren Castle Town
								break;
							case 7130:
								_activeChar.teleToLocation(85348, 16142, -3699, true); // Ivory Tower
								break;
							case 7131:
								_activeChar.teleToLocation(116819, 76994, -2714, true); // Hunters Village
								break;
							case 7132:
								_activeChar.teleToLocation(146331, 25762, -2018, true); // Aden Castle Town
								break;
							case 7133:
								_activeChar.teleToLocation(147928, -55273, -2734, true); // Goddard Castle Town
								break;
							case 7134:
								_activeChar.teleToLocation(43799, -47727, -798, true); // Rune Castle Town
								break;
							case 7135:
								_activeChar.teleToLocation(87331, -142842, -1317, true); // Schuttgart Castle Town
								break;
							case 7618:
								_activeChar.teleToLocation(149864, -81062, -5618, true); // Ketra Orc Village
								break;
							case 7619:
								_activeChar.teleToLocation(108275, -53785, -2524, true); // Varka Silenos Village
								break;
							case 9716:
								_activeChar.teleToLocation(-117251, 46771, 360, true); // Kamael Village
								break;
							default:
								_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
								break;
						}
					}
				}
			}
			catch (Throwable e)
			{
				if (Config.DEBUG)
					e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#getItemIds()
	 */
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
