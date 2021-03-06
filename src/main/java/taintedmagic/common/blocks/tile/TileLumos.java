package taintedmagic.common.blocks.tile;

import baubles.api.BaublesApi;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import taintedmagic.common.blocks.BlockLumos;
import taintedmagic.common.items.equipment.ItemLumosRing;
import taintedmagic.common.registry.ItemRegistry;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXSparkle;
import thaumcraft.common.items.wands.ItemWandCasting;

public class TileLumos extends TileEntity
{
	int ticksExisted;

	public TileLumos ()
	{
	}

	public boolean canUpdate ()
	{
		return true;
	}

	@Override
	public void updateEntity ()
	{
		ticksExisted++;

		Block b = worldObj.getBlock(xCoord, yCoord, zCoord);
		int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);

		if (b instanceof BlockLumos)
		{
			EntityPlayer p = worldObj.getClosestPlayer(xCoord, yCoord, zCoord, 4);

			if (meta == 0 && worldObj.rand.nextInt(15) == 0)
			{
				if (worldObj.isRemote) spawnParticles();
			}
			if ( (meta == 1 || meta == 2) && p == null)
			{
				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			}
			else if (meta == 1 && p != null)
			{
				if (p.getHeldItem() == null || (p.getHeldItem() != null && ! (p.getHeldItem().getItem() instanceof ItemWandCasting))) worldObj.setBlockToAir(xCoord, yCoord, zCoord);
				else if (p.getHeldItem() != null && p.getHeldItem().getItem() instanceof ItemWandCasting)
				{
					ItemStack s = p.getHeldItem();
					ItemWandCasting wand = (ItemWandCasting) s.getItem();

					if (wand.getFocus(s) == null || (wand.getFocus(s) != null && wand.getFocus(s) != ItemRegistry.ItemFocusLumos)) worldObj.setBlockToAir(xCoord, yCoord, zCoord);
				}
			}
			if (meta == 2 && p != null)
			{
				IInventory baub = BaublesApi.getBaubles(p);
				if (baub.getStackInSlot(1) == null && baub.getStackInSlot(2) == null) worldObj.setBlockToAir(xCoord, yCoord, zCoord);
				else if ( (baub.getStackInSlot(1) != null && baub.getStackInSlot(2) == null) && ! (baub.getStackInSlot(1).getItem() instanceof ItemLumosRing))
					worldObj.setBlockToAir(xCoord, yCoord, zCoord);
				else if ( (baub.getStackInSlot(1) == null && baub.getStackInSlot(2) != null) && ! (baub.getStackInSlot(2).getItem() instanceof ItemLumosRing))
					worldObj.setBlockToAir(xCoord, yCoord, zCoord);
				else if ( (baub.getStackInSlot(1) != null && baub.getStackInSlot(2) != null)
						&& (! (baub.getStackInSlot(1).getItem() instanceof ItemLumosRing) && ! (baub.getStackInSlot(2).getItem() instanceof ItemLumosRing)))
					worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			}
		}
	}

	@SideOnly (Side.CLIENT)
	void spawnParticles ()
	{
		FXSparkle fx = new FXSparkle(worldObj, (double) xCoord + 0.5D + (worldObj.rand.nextGaussian() * 0.1D), (double) yCoord + 0.5D + (worldObj.rand.nextGaussian() * 0.1D),
				(double) zCoord + 0.5D + (worldObj.rand.nextGaussian() * 0.1D), 1.75F, 6, 3 + worldObj.rand.nextInt(2));
		fx.slowdown = true;
		fx.setGravity(-0.5F);
		ParticleEngine.instance.addEffect(worldObj, fx);
	}
}