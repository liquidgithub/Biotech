package biotech.tileentity;

import java.util.ArrayList;
import java.util.List;

import liquidmechanics.api.IColorCoded;
import liquidmechanics.api.IPressure;
import liquidmechanics.api.helpers.ColorCode;
import liquidmechanics.api.helpers.LiquidData;
import liquidmechanics.api.helpers.LiquidHandler;


import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquidTank;
import net.minecraftforge.liquids.ITankContainer;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;
import universalelectricity.core.UniversalElectricity;
import universalelectricity.core.electricity.ElectricityNetwork;
import universalelectricity.core.electricity.ElectricityPack;
import universalelectricity.core.implement.IItemElectric;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.network.IPacketReceiver;
import universalelectricity.prefab.network.PacketManager;
import biotech.Biotech;

import com.google.common.io.ByteArrayDataInput;


public class MilkingMachineTileEntity extends BasicMachineTileEntity implements IPacketReceiver, IColorCoded, IPressure, ITankContainer
{
	private int tickCounter;
	private int scantickCounter;
	
	protected List<EntityLiving> CowList = new ArrayList<EntityLiving>();
	
	// Watts being used per action / idle action
	public static final double WATTS_PER_TICK = 250;
	public static final double WATTS_PER_IDLE_TICK = 25;
	
	// Time idle after a tick
	public static final int IDLE_TIME_AFTER_ACTION = 60;
	public static final int IDLE_TIME_NO_ACTION = 30;
	
	// Watts being used per pump action
	public static final double WATTS_PER_PUMP_ACTION = 50;
	
	//How much power is stored?
    private double electricityStored  = 0;
    private double electricityMaxStored  = 5000;
    
    private boolean isMilking = false;
    
    //Is the machine currently powered, and did it change?
    public boolean prevIsPowered, isPowered = false;
    public boolean ReceivedRedstone = false;
    
    //Amount of milliBuckets of internal storage
    private ColorCode color = ColorCode.WHITE;
 	private static final int MILK_CAPACITY_MILLIBUCKET = 300;
 	private int milkContentsMilliBuckets = 0;
 	private ILiquidTank milkSmallTank = new LiquidTank(Biotech.milkLiquid, MILK_CAPACITY_MILLIBUCKET, this);

	private int facing;
	private int playersUsing = 0;
	private int idleTicks;
	
    public int currentX = 0;
    public int currentZ = 0;
    public int currentY = 0;
	
    public int minX, maxX;
    public int minZ, maxZ;
	
	
	public MilkingMachineTileEntity()
	{
		super();
	}

	public void scanForCows()
	{
		int xminrange = xCoord- getScanRange();
		int xmaxrange = xCoord+ getScanRange()+1;
		int yminrange = yCoord- getScanRange();
		int ymaxrange = yCoord+ getScanRange()+1;
		int zminrange = zCoord- getScanRange();
		int zmaxrange = zCoord+ getScanRange()+1;
		
		List<EntityCow> scannedCowslist = worldObj.getEntitiesWithinAABB(EntityCow.class, AxisAlignedBB.getBoundingBox(xminrange, yminrange, zminrange, xmaxrange, ymaxrange, zmaxrange));

		for(EntityCow Living : scannedCowslist)
		{
			if(!CowList.contains(Living))
			{
				CowList.add(Living);
			}
		}	
		
	}
	
	public void milkCows()
	{		
		if(CowList.size() != 0)
		{
			CowList.remove(0);
			this.milkContentsMilliBuckets += 30;
		}
	}

	public int getScanRange() {
		return 3;
	}
	
	@Override
    public void updateEntity()
    {
		
        if (!worldObj.isRemote)
        {	        
	        if(this.ReceivedRedstone)
	        {
	        	this.setPowered(true);
	        	if(scantickCounter >= 40)
	        	{
	        		scanForCows();
	        		scantickCounter = 0;
	        	}
	        	if(this.getMilkStored() >= 30)
	        	{
	        		this.milkSmallTank.drain(30, true);
	        	}
	        	if(CowList.size() != 0 && tickCounter >= 100)
	        	{
	        		milkCows();
	    			isMilking = true;
	        		tickCounter = 0;
	        		this.setPowered(false);
	        	}
	        	System.out.println("Machine:"+ milkContentsMilliBuckets);
	            tickCounter++;
	            scantickCounter++;
	            LiquidStack stack = Biotech.milkLiquid;
	            if (this.milkSmallTank.getLiquid() != null)
                {
                    stack = this.milkSmallTank.getLiquid();
                }
	        }
	        if(tickCounter >= 150)
	        {
	        	tickCounter = 0;
	        }
	        if(scantickCounter >= 100)
	        {
	        	tickCounter = 0;
	        }
	        if (this.ticks % 3 == 0 && this.playersUsing > 0)
			{
				PacketManager.sendPacketToClients(getDescriptionPacket(), this.worldObj, new Vector3(this), 12);
			}
        
	        int front = 0; 
	    	
	    	switch(this.getFacing())
	    	{
	    	case 2:
	    		front = 3;
	    		break;
	    	case 3:
	    		front = 2;
	    		break;
	    	case 4:
	    		front = 5;
	    		break;
	    	case 5:
	    		front = 4;
	    		break;
	    	default:
	    		front = 3;
	   			break;
	    	}
	    	
	    	ForgeDirection direction = ForgeDirection.getOrientation(front);
	
	        TileEntity inputTile = Vector3.getTileEntityFromSide(this.worldObj, new Vector3(this), direction);
	        
	        ElectricityNetwork network = ElectricityNetwork.getNetworkFromTileEntity(inputTile, direction);
	        
	        if (inputTile != null && network != null)
	        {   
                if (this.electricityStored < this.electricityMaxStored)
                {
                	double electricityNeeded = this.electricityMaxStored - this.electricityStored; 
                	
                    network.startRequesting(this, electricityNeeded, electricityNeeded >= getVoltage() ? getVoltage() : electricityNeeded);

                    this.setElectricityStored(electricityStored + (network.consumeElectricity(this).getWatts()));
                    
                    if (UniversalElectricity.isVoltageSensitive)
    				{
                    	ElectricityPack electricityPack = network.consumeElectricity(this);
    					if (electricityPack.voltage > this.getVoltage())
    					{
    						this.worldObj.createExplosion(null, this.xCoord, this.yCoord, this.zCoord, 2f, true);
    					}
    				}
                    
                }
				else if(electricityStored >= electricityMaxStored)
                {
                    network.stopRequesting(this);
                }
	        }
	        
			if (this.inventory[0] != null && this.electricityStored < this.electricityMaxStored)
			{
				if (this.inventory[0].getItem() instanceof IItemElectric)
				{
					IItemElectric electricItem = (IItemElectric) this.inventory[0].getItem();

					if (electricItem.canProduceElectricity())
					{
						double joulesReceived = electricItem.onUse(electricItem.getMaxJoules(this.inventory[0]) * 0.005, this.inventory[0]);
						this.setElectricityStored(this.electricityStored + joulesReceived);
					}
				}
			}
        }
        super.updateEntity();
    }
	
	public boolean GetRedstoneSignal()
	{
		return ReceivedRedstone;
	}

	@Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        
        this.facing = tagCompound.getShort("facing");
        this.isPowered = tagCompound.getBoolean("isPowered");
        this.electricityStored = tagCompound.getDouble("electricityStored");
        LiquidStack liquid = Biotech.milkLiquid;
        ((LiquidTank) milkSmallTank).setLiquid(liquid);
        
        NBTTagList tagList = tagCompound.getTagList("Inventory");
        for (int i = 0; i < tagList.tagCount(); i++)
        {
            NBTTagCompound tag = (NBTTagCompound) tagList.tagAt(i);
            byte slot = tag.getByte("Slot");

            if (slot >= 0 && slot < inventory.length)
            {
                inventory[slot] = ItemStack.loadItemStackFromNBT(tag);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);

        tagCompound.setShort("facing", (short)this.facing);
        tagCompound.setBoolean("isPowered", this.isPowered);
        tagCompound.setDouble("electricityStored", this.electricityStored);
        if (milkSmallTank.getLiquid() != null)
        {
            tagCompound.setTag("stored", milkSmallTank.getLiquid().writeToNBT(new NBTTagCompound()));
        }
        
        NBTTagList itemList = new NBTTagList();
        for (int i = 0; i < inventory.length; i++)
        {
            ItemStack stack = inventory[i];

            if (stack != null)
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte) i);
                stack.writeToNBT(tag);
                itemList.appendTag(tag);
            }
        }
        tagCompound.setTag("Inventory", itemList);
    }

    @Override
    public String getInvName()
    {
        return "Milking Machine";
    }
    
    @Override
	public void handlePacketData(INetworkManager network, int packetType, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream) 
	{
		try
		{
			if (this.worldObj.isRemote)
			{
				this.isPowered = dataStream.readBoolean();
				this.facing = dataStream.readInt();
				this.electricityStored = dataStream.readDouble();
				this.ReceivedRedstone = dataStream.readBoolean();
				((LiquidTank) this.milkSmallTank).setLiquid(new LiquidStack(dataStream.readInt(), dataStream.readInt(), dataStream.readInt()));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public Packet getDescriptionPacket()
	{
		return PacketManager.getPacket(Biotech.CHANNEL, this, this.isPowered, this.facing, this.electricityStored, this.ReceivedRedstone);
	}
	
	public int getFacing() 
	{
		return facing;
	}

	public void setFacing(int facing) 
	{
		this.facing = facing;
	}
    
    public double getElectricityStored() 
	{
		return electricityStored;
	}

	public void setElectricityStored(double joules)
	{
		electricityStored = Math.max(Math.min(joules, getMaxElectricity()), 0);
	}	
	
	public double getMaxElectricity() 
	{
		return electricityMaxStored;
	}
	
	public int getMilkStored()
    {
    	return this.milkContentsMilliBuckets;
    }

	@Override
	public ColorCode getColor() 
	{
		return ColorCode.WHITE;
	}

	@Override
	public void setColor(Object obj) 
	{
		this.color = ColorCode.WHITE;
	}

	@Override
	public int presureOutput(LiquidData type, ForgeDirection dir) 
	{
		if (type == color.getLiquidData() || type == LiquidHandler.unkown)
        {
            if (type.getCanFloat() && dir == ForgeDirection.DOWN)
                return type.getPressure();
        }
        return 0;
	}

	@Override
	public boolean canPressureToo(LiquidData type, ForgeDirection dir) 
	{
		if (type == color.getLiquidData() || type == LiquidHandler.unkown)
        {
            if (type.getCanFloat() && dir == ForgeDirection.DOWN)
                return true;
        }
        return false;
	}

	@Override
	public int fill(ForgeDirection from, LiquidStack resource, boolean doFill) {
		if(from == ForgeDirection.DOWN && resource == Biotech.milkLiquid)
		{
			int availibleCapactity = MILK_CAPACITY_MILLIBUCKET - milkContentsMilliBuckets;
			int inputAmount = resource.amount;
			if (inputAmount <= availibleCapactity) 
			{
				if (doFill) 
				{
					this.milkContentsMilliBuckets += inputAmount;
				}
				return inputAmount;
			} 
			else 
			{
				if (doFill) 
				{
					this.milkContentsMilliBuckets = MILK_CAPACITY_MILLIBUCKET;
				}
				return availibleCapactity;
			}
		}
		else
		{
			return 0;
		}
	}

	@Override
	public int fill(int tankIndex, LiquidStack resource, boolean doFill) {
		return 0;
	}

	@Override
	public LiquidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public LiquidStack drain(int tankIndex, int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public ILiquidTank[] getTanks(ForgeDirection direction) {
		return new ILiquidTank[] { milkSmallTank };
	}

	@Override
	public ILiquidTank getTank(ForgeDirection direction, LiquidStack type) {
		if ((direction == ForgeDirection.DOWN) && type.isLiquidEqual(Biotech.milkLiquid)) 
		{
			return this.milkSmallTank; 
		}
		return null;
	}
}