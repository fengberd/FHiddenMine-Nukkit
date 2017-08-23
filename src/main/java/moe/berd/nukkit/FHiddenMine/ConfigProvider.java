package moe.berd.nukkit.FHiddenMine;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import cn.nukkit.utils.*;

public class ConfigProvider
{
	private static Config config=null;
	private static ConfigSection defaults=new ConfigSection()
	{
		{
			set("FillMode",false);
			set("ScanHeight",3);
			set("ProtectWorlds",new ArrayList<String>());
			set("Ores",new ArrayList<Integer>()
			{
				{
					add(14);
					add(15);
					add(16);
					add(21);
					add(56);
					add(73);
					add(74);
					add(129);
				}
			});
			set("Filters",new ArrayList<Integer>()
			{
				{
					add(0);
					add(8);
					add(9);
					add(10);
					add(11);
					add(20);
					add(26);
					add(27);
					add(30);
					add(31);
					add(32);
					add(37);
					add(38);
					add(39);
					add(40);
					add(44);
					add(50);
					add(63);
					add(64);
					add(65);
					add(66);
					add(68);
					add(71);
					add(81);
					add(83);
					add(85);
					add(96);
					add(101);
					add(102);
					add(104);
					add(105);
					add(106);
					add(107);
					add(126);
					add(141);
					add(142);
				}
			});
		}
	};
	
	public static Config getConfig()
	{
		return config;
	}
	
	public static ConfigSection getDefaults()
	{
		return new ConfigSection(defaults);
	}
	
	public static boolean validateIndex(String index)
	{
		return defaults.containsKey(index);
	}
	
	public static void init(Main main)
	{
		main.getDataFolder().mkdirs();
		config=new Config(new File(main.getDataFolder(),"config.yml").toString(),Config.YAML,defaults);
		ConfigSection data=getDefaults();
		for(String key : config.getAll().keySet())
		{
			data.set(key,config.get(key,data.get(key)));
		}
		data.set("ProtectWorlds",data.getStringList("ProtectWorlds").stream()
				.map(String::toLowerCase)
				.collect(Collectors.toList()));
		config.setAll(data);
		config.save();
	}
	
	public static void save()
	{
		config.save();
	}
	
	public static void set(String key,Object value)
	{
		if(!validateIndex(key))
		{
			throw new RuntimeException("Invalid config index.");
		}
		config.set(key,value);
		config.save();
	}
	
	public static int getInt(String key)
	{
		return config.getInt(key,defaults.getInt(key));
	}
	
	public static double getDouble(String key)
	{
		return config.getDouble(key,defaults.getDouble(key));
	}
	
	public static String getString(String key)
	{
		return config.getString(key,defaults.getString(key));
	}
	
	public static boolean getBoolean(String key)
	{
		return config.getBoolean(key,defaults.getBoolean(key));
	}
	
	public static List<Integer> getIntegerList(String key)
	{
		return config.getIntegerList(key);
	}
	
	public static List<String> getStringList(String key)
	{
		return config.getStringList(key);
	}
}
