package taintedmagic.common.handler;

import java.util.Random;
import java.util.UUID;

import baubles.api.BaublesApi;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import taintedmagic.common.items.equipment.ItemLumosRing;
import taintedmagic.common.items.tools.ItemHollowDagger;
import taintedmagic.common.items.wand.foci.ItemFocusMageMace;
import taintedmagic.common.registry.BlockRegistry;
import taintedmagic.common.registry.ItemRegistry;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.Config;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.network.playerdata.PacketResearchComplete;

public class TMEventHandler
{
	Random randy = new Random();

	@SubscribeEvent
	public void playerTick (LivingEvent.LivingUpdateEvent event)
	{
		if (event.entity instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer) event.entity;
			modifyAttackDamage(p);

			/**
			 * Repair voidtouched items
			 */
			for (int i = 0; i < p.inventory.getSizeInventory(); i++)
			{
				ItemStack s = p.inventory.getStackInSlot(i);
				if (s != null)
				{
					if (s.stackTagCompound != null && s.stackTagCompound.getBoolean("voidtouched"))
					{
						if (!p.worldObj.isRemote && s.isItemDamaged() && p.ticksExisted % 20 == 0)
						{
							s.setItemDamage(s.getItemDamage() - 1);
						}
					}
				}
			}

			/**
			 * Create lumos blocks when the player is holding a wand or staff
			 * with the lumos focus equipped or when the Lumos ring is equipped.
			 */
			if (p.getHeldItem() != null && p.getHeldItem().getItem() instanceof ItemWandCasting)
			{
				ItemStack s = p.getHeldItem();
				ItemWandCasting wand = (ItemWandCasting) s.getItem();

				if (wand.getFocus(s) != null && wand.getFocus(s) == ItemRegistry.ItemFocusLumos && !p.worldObj.isRemote) createLumos(p, 1);
			}
			IInventory baub = BaublesApi.getBaubles(p);
			if ( (baub.getStackInSlot(1) != null && baub.getStackInSlot(1).getItem() instanceof ItemLumosRing)
					|| (baub.getStackInSlot(2) != null && baub.getStackInSlot(2).getItem() instanceof ItemLumosRing))
				createLumos(p, 2);
		}
	}

	/**
	 * Creates lumos lightsource blocks.
	 */
	public void createLumos (EntityPlayer p, int meta)
	{
		int x = Math.round((float) p.posX);
		int y = Math.round((float) p.posY) + 1;
		int z = Math.round((float) p.posZ);

		if (p.worldObj.getBlock(x, y, z) == Blocks.air) p.worldObj.setBlock(x, y, z, BlockRegistry.BlockLumos, meta, 3);
	}

	/*
	 * Some hacky code to make the mage's mace work...
	 */
	public void modifyAttackDamage (EntityPlayer p)
	{
		if (!p.worldObj.isRemote)
		{
			IInventory inv = p.inventory;

			for (int i = 0; i < inv.getSizeInventory(); i++)
			{
				if (inv.getStackInSlot(i) != null && inv.getStackInSlot(i).getItem() instanceof ItemWandCasting)
				{
					ItemStack s = inv.getStackInSlot(i);
					ItemWandCasting wand = (ItemWandCasting) inv.getStackInSlot(i).getItem();

					if (wand.getFocus(s) != null && wand.getFocus(s) == ItemRegistry.ItemFocusMageMace && wand.getRod(s) instanceof WandRod)
					{
						NBTTagList tags = new NBTTagList();
						NBTTagCompound tag = new NBTTagCompound();
						tag.setString("AttributeName", SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName());

						UUID u = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
						AttributeModifier am = new AttributeModifier(u, "Weapon modifier", ConfigHandler.baseMaceDamageIncrease + wand.getFocusPotency(s), 0);

						tag.setString("Name", am.getName());
						tag.setDouble("Amount", am.getAmount());
						tag.setInteger("Operation", am.getOperation());
						tag.setLong("UUIDMost", am.getID().getMostSignificantBits());
						tag.setLong("UUIDLeast", am.getID().getLeastSignificantBits());

						tags.appendTag(tag);
						s.stackTagCompound.setTag("AttributeModifiers", tags);
					}
					else if (wand.getRod(s) instanceof WandRod)
					{
						if (!s.hasTagCompound())
						{
							s.setTagCompound(new NBTTagCompound());
						}
						s.stackTagCompound.removeTag("AttributeModifiers");
					}
					if (wand.getFocus(s) != null && wand.getFocus(s) == ItemRegistry.ItemFocusMageMace && wand.getRod(s) instanceof StaffRod)
					{
						NBTTagList tags = new NBTTagList();
						NBTTagCompound tag = new NBTTagCompound();
						tag.setString("AttributeName", SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName());

						UUID u = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
						AttributeModifier am = new AttributeModifier(u, "Weapon modifier", 6.0D + ConfigHandler.baseMaceDamageIncrease + wand.getFocusPotency(s), 0);

						tag.setString("Name", am.getName());
						tag.setDouble("Amount", am.getAmount());
						tag.setInteger("Operation", am.getOperation());
						tag.setLong("UUIDMost", am.getID().getMostSignificantBits());
						tag.setLong("UUIDLeast", am.getID().getLeastSignificantBits());

						tags.appendTag(tag);
						s.stackTagCompound.setTag("AttributeModifiers", tags);
					}
					else if (wand.getRod(s) instanceof StaffRod)
					{
						NBTTagList tags = new NBTTagList();
						NBTTagCompound tag = new NBTTagCompound();
						tag.setString("AttributeName", SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName());

						UUID u = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
						AttributeModifier am = new AttributeModifier(u, "Weapon modifier", 6.0D, 0);

						tag.setString("Name", am.getName());
						tag.setDouble("Amount", am.getAmount());
						tag.setInteger("Operation", am.getOperation());
						tag.setLong("UUIDMost", am.getID().getMostSignificantBits());
						tag.setLong("UUIDLeast", am.getID().getLeastSignificantBits());

						tags.appendTag(tag);
						s.stackTagCompound.setTag("AttributeModifiers", tags);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void entityAttacked (LivingAttackEvent event)
	{
		if (event.source.getEntity() instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer) event.source.getEntity();

			/**
			 * Consume vis for Mage's Mace hits
			 */
			if (p.getHeldItem() != null && p.getHeldItem().getItem() instanceof ItemWandCasting)
			{
				ItemStack s = p.getHeldItem();
				ItemWandCasting wand = (ItemWandCasting) s.getItem();

				if (wand.getFocus(s) != null && wand.getFocus(s) instanceof ItemFocusMageMace)
				{
					final AspectList aspects = new AspectList().add(Aspect.EARTH, 20).add(Aspect.ENTROPY, 20).add(Aspect.ORDER, 20);
					if (wand.consumeAllVis(s, p, aspects, true, false))
					{
						wand.consumeAllVis(s, p, aspects, true, false);
					}
					else
					{
						event.setCanceled(true);
					}
				}
			}

			/**
			 * Fill phials with blood when an entity is hit with the Hollow
			 * Dagger
			 */
			if (p.getHeldItem() != null && p.getHeldItem().getItem() instanceof ItemHollowDagger)
			{
				for (int i = 0; i < p.inventory.getSizeInventory(); i++)
				{
					if (p.inventory.getStackInSlot(i) != null && p.inventory.getStackInSlot(i).getItem() instanceof ItemEssence && p.inventory.getStackInSlot(i).getItemDamage() == 0)
					{
						p.inventory.decrStackSize(i, 1);

						if (p.inventory.addItemStackToInventory(new ItemStack(ItemRegistry.ItemMaterial, 1, 7)) == false)
							if (!p.worldObj.isRemote) p.entityDropItem(new ItemStack(ItemRegistry.ItemMaterial, 1, 7), 2.0F);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLogin (PlayerEvent.PlayerLoggedInEvent event)
	{
		if (UpdateHandler.show) event.player.addChatMessage(new ChatComponentText(UpdateHandler.updateStatus));
	}

	@SubscribeEvent
	public void onCrafting (ItemCraftedEvent event)
	{
		if (event.crafting.getItem() == ItemRegistry.ItemMaterial && event.crafting.getItemDamage() == 5) giveResearch(event.player);
	}

	public void giveResearch (EntityPlayer p)
	{
		if (!p.worldObj.isRemote)
		{
			if (!ThaumcraftApiHelper.isResearchComplete(p.getCommandSenderName(), "CREATION") && ThaumcraftApiHelper.isResearchComplete(p.getCommandSenderName(), "CREATIONSHARD"))
			{
				Thaumcraft.proxy.getResearchManager().completeResearch(p, "CREATION");
				thaumcraft.common.lib.network.PacketHandler.INSTANCE.sendTo(new PacketResearchComplete("CREATION"), (EntityPlayerMP) p);
				p.addChatMessage(new ChatComponentText("\u00A75" + StatCollector.translateToLocal("text.creation")));
				p.playSound("thaumcraft:wind", 1.0F, 5.0F);

				if (!ThaumcraftApiHelper.isResearchComplete(p.getCommandSenderName(), "OUTERREV") && ThaumcraftApiHelper.isResearchComplete(p.getCommandSenderName(), "CREATION"))
				{
					Thaumcraft.proxy.getResearchManager().completeResearch(p, "OUTERREV");
					thaumcraft.common.lib.network.PacketHandler.INSTANCE.sendTo(new PacketResearchComplete("OUTERREV"), (EntityPlayerMP) p);
				}

				try
				{
					p.addPotionEffect(new PotionEffect(Potion.blindness.id, 80, 0));
					p.addPotionEffect(new PotionEffect(Config.potionBlurredID, 200, 0));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	@SubscribeEvent
	public void livingDrop (LivingDropsEvent event)
	{
		if (event.source.getEntity() instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer) event.source.getEntity();

			// Currently unused
		}
	}

	public void addDropItem (LivingDropsEvent event, ItemStack drop)
	{
		EntityItem entityitem = new EntityItem(event.entityLiving.worldObj, event.entityLiving.posX, event.entityLiving.posY, event.entityLiving.posZ, drop);
		entityitem.delayBeforeCanPickup = 10;
		event.drops.add(entityitem);
	}

	@SubscribeEvent
	public void tooltipEvent (ItemTooltipEvent event)
	{
		if (event.itemStack.getItem() instanceof ItemFocusMageMace)
		{
			if (event.toolTip.contains(StatCollector.translateToLocal("item.Focus.cost1")))
			{
				event.toolTip.remove(StatCollector.translateToLocal("item.Focus.cost1"));
				event.toolTip.add(1, StatCollector.translateToLocal("item.Focus.cost3"));
			}
		}

		if (event.itemStack.stackTagCompound != null && event.itemStack.stackTagCompound.getBoolean("voidtouched")) event.toolTip.add("\u00A75" + StatCollector.translateToLocal("text.voidtouched"));
	}
}
