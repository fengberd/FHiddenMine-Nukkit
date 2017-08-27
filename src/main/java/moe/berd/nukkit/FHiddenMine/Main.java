package moe.berd.nukkit.FHiddenMine;

import cn.nukkit.*;
import cn.nukkit.nbt.*;
import cn.nukkit.math.*;
import cn.nukkit.utils.*;
import cn.nukkit.event.*;
import cn.nukkit.level.*;
import cn.nukkit.plugin.*;
import cn.nukkit.blockentity.*;

import cn.nukkit.nbt.tag.*;
import cn.nukkit.event.block.*;
import cn.nukkit.event.player.*;
import cn.nukkit.level.format.anvil.*;
import cn.nukkit.level.format.generic.*;
import cn.nukkit.level.format.ChunkSection;

import java.io.*;
import java.nio.*;
import java.util.*;

public class Main extends PluginBase implements Listener
{
	private static Main obj;
	
	public static final String PERMISSION_SHOW="FHiddenMine.show";
	
	public static Main getInstance()
	{
		return obj;
	}
	
	private int scanHeight=0;
	private List<String> protectWorlds=null;
	private List<Integer> ores=null, filter=null;
	
	private boolean fillMode=false;
	
	@Override
	public void onEnable()
	{
		obj=this;
		reload();
		this.getServer().getPluginManager().registerEvents(this,this);
	}
	
	public void reload()
	{
		ConfigProvider.init(this);
		fillMode=ConfigProvider.getBoolean("FillMode");
		scanHeight=ConfigProvider.getInt("ScanHeight");
		protectWorlds=ConfigProvider.getStringList("ProtectWorlds");
		ores=ConfigProvider.getIntegerList("Ores");
		filter=ConfigProvider.getIntegerList("Filters");
	}
	
	@SuppressWarnings("SuspiciousMethodCalls")
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerChunkRequest(PlayerChunkRequestEvent event)
	{
		Player player=event.getPlayer();
		Level level=player.getLevel();
		if(player.hasPermission(PERMISSION_SHOW) || !protectWorlds.contains(level.getFolderName().toLowerCase()))
		{
			return;
		}
		event.setCancelled();
		BaseFullChunk chunk=level.getChunk(event.getChunkX(),event.getChunkZ(),false);
		byte[] tiles=new byte[0];
		if(!chunk.getBlockEntities().isEmpty())
		{
			List<CompoundTag> tagList=new ArrayList<>();
			for(BlockEntity blockEntity : chunk.getBlockEntities().values())
			{
				if(blockEntity instanceof BlockEntitySpawnable)
				{
					tagList.add(((BlockEntitySpawnable)blockEntity).getSpawnCompound());
				}
			}
			try
			{
				tiles=NBTIO.write(tagList,ByteOrder.LITTLE_ENDIAN,true);
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		Map<Integer,Integer> extra=chunk.getBlockExtraDataArray();
		BinaryStream extraData;
		if(!extra.isEmpty())
		{
			extraData=new BinaryStream();
			extraData.putLInt(extra.size());
			for(Integer key : extra.values())
			{
				extraData.putLInt(key);
				extraData.putLShort(extra.get(key));
			}
		}
		else
		{
			extraData=null;
		}
		BinaryStream stream=new BinaryStream();
		int fillIndex=0;
		if(chunk.getProvider() instanceof Anvil)
		{
			int count=0;
			ChunkSection[] sections=((cn.nukkit.level.format.anvil.Chunk)chunk).getSections();
			for(int i=sections.length-1;i>=0;i--)
			{
				if(!sections[i].isEmpty())
				{
					count=i+1;
					break;
				}
			}
			stream.putByte((byte)count);
			for(int i=0;i<count;i++)
			{
				ChunkSection section=sections[i];
				ByteBuffer buffer=ByteBuffer.allocate(10240);
				byte[] blocks=new byte[4096];
				byte[] data=new byte[2048];
				byte[] skyLight=new byte[2048];
				byte[] blockLight=new byte[2048];
				for(int x=0;x<16;x++)
				{
					for(int z=0;z<16;z++)
					{
						int index=(x << 7) | (z << 3);
						for(int y=0;y<16;y+=2)
						{
							int b1=0, b2=0, tmpId, x_=chunk.getX()*16, y_=section.getY()*16, z_=chunk.getZ()*16;
							if(y_<scanHeight && !filter.contains(level.getBlockIdAt(x_+x+1,y_+y,z_+z)) && !filter.contains(level.getBlockIdAt(x_+x-1,y_+y,z_+z)) &&
									!filter.contains(level.getBlockIdAt(x_+x,y_+y+1,z_+z)) && !filter.contains(level.getBlockIdAt(x_+x,y_+y-1,z_+z)) &&
									!filter.contains(level.getBlockIdAt(x_+x,y_+y,z_+z+1)) && !filter.contains(level.getBlockIdAt(x_+x,y_+y,z_+z-1)))
							{
								if(fillMode)
								{
									blocks[(index << 1) | y]=(byte)(int)ores.get(++fillIndex%ores.size());
								}
								else if(ores.contains(tmpId=section.getBlockId(x,y,z)))
								{
									blocks[(index << 1) | y]=1;
								}
								else
								{
									blocks[(index << 1) | y]=(byte)tmpId;
									b1=section.getBlockData(x,y,z);
								}
							}
							else
							{
								blocks[(index << 1) | y]=(byte)section.getBlockId(x,y,z);
								b1=section.getBlockData(x,y,z);
							}
							++y_;
							if(y_<scanHeight && !filter.contains(level.getBlockIdAt(x_+x+1,y_+y,z_+z)) && !filter.contains(level.getBlockIdAt(x_+x-1,y_+y,z_+z)) &&
									!filter.contains(level.getBlockIdAt(x_+x,y_+y+1,z_+z)) && !filter.contains(level.getBlockIdAt(x_+x,y_+y-1,z_+z)) &&
									!filter.contains(level.getBlockIdAt(x_+x,y_+y,z_+z+1)) && !filter.contains(level.getBlockIdAt(x_+x,y_+y,z_+z-1)))
							{
								if(fillMode)
								{
									blocks[(index << 1) | (y+1)]=(byte)(int)ores.get(++fillIndex%ores.size());
								}
								else if(ores.contains(tmpId=section.getBlockId(x,y+1,z)))
								{
									blocks[(index << 1) | (y+1)]=1;
								}
								else
								{
									blocks[(index << 1) | (y+1)]=(byte)tmpId;
									b2=section.getBlockData(x,y+1,z);
								}
							}
							else
							{
								blocks[(index << 1) | (y+1)]=(byte)section.getBlockId(x,y+1,z);
								b1=section.getBlockData(x,y+1,z);
							}
							data[index | (y >> 1)]=(byte)((b2 << 4) | b1);
							b1=section.getBlockSkyLight(x,y,z);
							b2=section.getBlockSkyLight(x,y+1,z);
							skyLight[index | (y >> 1)]=(byte)((b2 << 4) | b1);
							b1=section.getBlockLight(x,y,z);
							b2=section.getBlockLight(x,y+1,z);
							blockLight[index | (y >> 1)]=(byte)((b2 << 4) | b1);
						}
					}
				}
				stream.putByte((byte)0);
				stream.put(blocks);
				stream.put(data);
				stream.put(skyLight);
				stream.put(blockLight);
			}
			for(int height : chunk.getHeightMapArray())
			{
				stream.putByte((byte)height);
			}
			stream.put(new byte[256]);
			stream.put(chunk.getBiomeIdArray());
			stream.putByte((byte)0);
			if(extraData!=null)
			{
				stream.put(extraData.getBuffer());
			}
			else
			{
				stream.putVarInt(0);
			}
		}
		else
		{
			byte[] blocks=chunk.getBlockIdArray(), blocksData=chunk.getBlockDataArray();
			for(int x=0;x<16;x++)
			{
				for(int z=0;z<16;z++)
				{
					for(int y=0;y<scanHeight;y++)
					{
						int index=(x << 11) | (z << 7) | y;
						if(!filter.contains(level.getBlockIdAt(x+1,y,z)) && !filter.contains(level.getBlockIdAt(x-1,y,z)) &&
								!filter.contains(level.getBlockIdAt(x,y+1,z)) && !filter.contains(level.getBlockIdAt(x,y-1,z)) &&
								!filter.contains(level.getBlockIdAt(x,y,z+1)) && !filter.contains(level.getBlockIdAt(x,y,z-1)))
						{
							if(fillMode)
							{
								blocks[index]=(byte)(int)ores.get(++fillIndex%ores.size());
								blocksData[index]=0;
							}
							else if(ores.contains(blocks[index]))
							{
								blocks[index]=1;
								blocksData[index]=0;
							}
						}
					}
				}
			}
			stream.put(blocks);
			stream.put(blocksData);
			stream.put(chunk.getBlockSkyLightArray());
			stream.put(chunk.getBlockLightArray());
			for(int height : chunk.getHeightMapArray())
			{
				stream.putByte((byte)height);
			}
			for(int color : chunk.getBiomeColorArray())
			{
				stream.put(Binary.writeInt(color));
			}
			if(extraData!=null)
			{
				stream.put(extraData.getBuffer());
			}
			else
			{
				stream.putLInt(0);
			}
		}
		stream.put(tiles);
		player.sendChunk(chunk.getX(),chunk.getZ(),stream.getBuffer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event)
	{
		Player player=event.getPlayer();
		Level level=player.getLevel();
		if(player.hasPermission(PERMISSION_SHOW) || !protectWorlds.contains(level.getFolderName().toLowerCase()))
		{
			return;
		}
		Vector3 pos=event.getBlock();
		level.sendBlocks(new Player[]{player},new Vector3[]
		{
			pos.add(1),pos.add(-1),
			pos.add(0,1),pos.add(0,-1),
			pos.add(0,0,1),pos.add(0,0,-1)
		});
	}
}
