/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.proxy;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import buildcraft.api.enums.EnumColor;
import buildcraft.core.EntityLaser;
import buildcraft.core.client.BuildCraftStateMapper;
import buildcraft.core.lib.EntityBlock;
import buildcraft.core.lib.engines.RenderEngine;
import buildcraft.core.lib.engines.TileEngineBase;
import buildcraft.core.lib.render.RenderEntityBlock;
import buildcraft.core.lib.utils.IModelRegister;
import buildcraft.core.lib.utils.Utils;
import buildcraft.core.render.RenderLaser;

public class CoreProxyClient extends CoreProxy {

    /* INSTANCES */
    @Override
    public Object getClient() {
        return FMLClientHandler.instance().getClient();
    }

    @Override
    public World getClientWorld() {
        return FMLClientHandler.instance().getClient().theWorld;
    }

    /* ENTITY HANDLING */
    @Override
    public void removeEntity(Entity entity) {
        super.removeEntity(entity);

        if (entity.worldObj.isRemote) {
            ((WorldClient) entity.worldObj).removeEntityFromWorld(entity.getEntityId());
        }
    }

    /* WRAPPER */
    @SuppressWarnings("rawtypes")
    @Override
    public void feedSubBlocks(Block block, CreativeTabs tab, List itemList) {
        if (block == null) {
            return;
        }

        block.getSubBlocks(Item.getItemFromBlock(block), tab, itemList);
    }

    @Override
    public String getItemDisplayName(ItemStack stack) {
        if (stack.getItem() == null) {
            return "";
        }

        return stack.getDisplayName();
    }

    @Override
    public void initializeRendering() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEngineBase.class, new RenderEngine());
    }

    @Override
    public void initializeEntityRendering() {
        RenderingRegistry.registerEntityRenderingHandler(EntityBlock.class, RenderEntityBlock.INSTANCE);
        RenderingRegistry.registerEntityRenderingHandler(EntityLaser.class, new RenderLaser());
        EnumColor.registerIcons();

        for (Block block : blocksToRegisterRenderersFor) {
            if (block instanceof IModelRegister) {
                ((IModelRegister) block).registerModels();
                continue;
            }

            IBlockState defaultState = block.getDefaultState();
            Multimap<Integer, IBlockState> metaStateMap = ArrayListMultimap.create();
            Map<IBlockState, String> stateTypeMap = Maps.newHashMap();
            BlockState blockState = block.getBlockState();
            if (blockState instanceof ExtendedBlockState) {
                // blockState.
            }

            for (IBlockState state : (List<IBlockState>) block.getBlockState().getValidStates()) {
                String type = BuildCraftStateMapper.getPropertyString(state);
                // for (IProperty property : (Collection<IProperty>) state.getProperties().keySet()) {
                // if (type.length() != 0)
                // type += ",";
                // type += property.getName() + "=";
                // Object value = state.getValue(property);
                // if (value instanceof Integer) {
                // type += ((Integer) value).intValue();
                // } else if (value instanceof Boolean) {
                // type += ((Boolean) value).toString();
                // } else if (value instanceof IStringSerializable) {
                // type += ((IStringSerializable) value).getName();
                // } else {
                // type += value.toString().toLowerCase();
                // }
                // }
                stateTypeMap.put(state, type);
                metaStateMap.put(block.damageDropped(state), state);
                // ModelBakery.addVariantName(Item.getItemFromBlock(block), type.toLowerCase());
            }
            for (Entry<Integer, Collection<IBlockState>> entry : metaStateMap.asMap().entrySet()) {
                Collection<IBlockState> blockStates = entry.getValue();
                if (blockStates.isEmpty())
                    continue;
                if (blockStates.contains(defaultState)) {
                    registerBlockItemModel(defaultState, entry.getKey(), stateTypeMap.get(defaultState));
                } else {
                    IBlockState state = blockStates.iterator().next();
                    registerBlockItemModel(state, entry.getKey(), stateTypeMap.get(state));
                }
            }
        }
        for (Item item : itemsToRegisterRenderersFor) {
            if (item instanceof IModelRegister) {
                ((IModelRegister) item).registerModels();
            }
        }
    }

    private void registerBlockItemModel(IBlockState state, int meta, String type) {
        Block block = state.getBlock();
        ModelResourceLocation location = new ModelResourceLocation(Utils.getNameForBlock(block).replace("|", ""), type.toLowerCase());
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), meta, location);
    }

    /* BUILDCRAFT PLAYER */
    @Override
    public String playerName() {
        return FMLClientHandler.instance().getClient().thePlayer.getDisplayNameString();
    }

    /** This function returns either the player from the handler if it's on the server, or directly from the minecraft
     * instance if it's the client. */
    @Override
    public EntityPlayer getPlayerFromNetHandler(INetHandler handler) {
        if (handler instanceof NetHandlerPlayServer) {
            return ((NetHandlerPlayServer) handler).playerEntity;
        } else {
            return Minecraft.getMinecraft().thePlayer;
        }
    }

    private LinkedList<Block> blocksToRegisterRenderersFor = new LinkedList<Block>();
    private LinkedList<Item> itemsToRegisterRenderersFor = new LinkedList<Item>();

    @Override
    public void registerBlock(Block block, Class<? extends ItemBlock> item) {
        super.registerBlock(block, item);
        blocksToRegisterRenderersFor.add(block);
        ModelLoader.setCustomStateMapper(block, BuildCraftStateMapper.INSTANCE);
    }

    @Override
    public void registerItem(Item item) {
        super.registerItem(item);
        itemsToRegisterRenderersFor.add(item);
    }
}