package gigaherz.biotech;

import gigaherz.biotech.client.TillingMachineGui;
import gigaherz.biotech.client.WorkerGui;
import gigaherz.biotech.container.BasicWorkerContainer;
import gigaherz.biotech.container.TillingMachineContainer;
import gigaherz.biotech.tileentity.BasicWorkerTileEntity;
import gigaherz.biotech.tileentity.TillingMachineTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler
{
	public void preInit()
	{
		// Preload textures
		MinecraftForgeClient.preloadTexture(Biotech.BLOCK_TEXTURE_FILE);
		MinecraftForgeClient.preloadTexture(Biotech.ITEM_TEXTURE_FILE);
		
		MinecraftForgeClient.preloadTexture(Biotech.FILE_PATH + "tillingmachine.png");
	}
	
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (tileEntity instanceof TillingMachineTileEntity)
        {
            return new TillingMachineContainer(player.inventory, (TillingMachineTileEntity) tileEntity);
        }

        return null;
    }
    
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
		    TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
		
		    if (tileEntity instanceof TillingMachineTileEntity)
		    {
		        return new TillingMachineGui(player.inventory, (TillingMachineTileEntity) tileEntity);
		    }

        return null;
    }
}