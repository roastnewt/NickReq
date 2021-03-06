package com.gmail.cwatterson.NickReq;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;


public class NickReq extends JavaPlugin implements Listener {
	
	private NickReqDAO _dao;
	private ArrayList<String> _blacklist;
	private String _website;
	
	public final NickReq plugin;
	
	public NickReq() 
	{
		
		this._dao = new NickReqDAO();
		this.plugin = this;
		
	}
	

	@Override
	public void onEnable()
	{

		// Check the configuration
		if (!new File(this.getDataFolder()+"/config.yml").exists())
		{
			this.saveDefaultConfig();
		}
		
		FileConfiguration config = this.getConfig();
		if ( !config.contains( "hostname" ) ) config.set( "hostname", "localhost" );
		if ( !config.contains( "database" ) ) config.set( "database", "nickreq" );
		if ( !config.contains( "username" ) ) config.set( "username", "nickrequser" );
		if ( !config.contains( "password" ) ) config.set( "password", "testpassword" );
		ArrayList<String> constructedDefaultList = new ArrayList<String>();
		constructedDefaultList.add("fuck");
		constructedDefaultList.add("shit");
		constructedDefaultList.add("fag");
        constructedDefaultList.add("damn");
        constructedDefaultList.add("bitch");
        constructedDefaultList.add("pussy");
        constructedDefaultList.add("asshole");
        constructedDefaultList.add("bastard");
        constructedDefaultList.add("slut");
        constructedDefaultList.add("douche");
		if ( !config.contains( "blacklist" ) ) config.set( "blacklist", constructedDefaultList);
		if ( !config.contains( "donationWebsite" ) ) config.set( "donationWebsite", "http://pure-realms.com/shop");
		this.saveConfig();
		
		// Connect to the database
		this._dao.connect( config.getString( "hostname", "localhost" ), 
				config.getString( "database", "nickreq" ), 
				config.getString( "username", "username" ), 
				config.getString( "password", "" ),
				this.getDataFolder() );
		
		this._blacklist = (ArrayList<String>)config.getList("blacklist");
		this._website = config.getString("donationWebsite");
		
		
		// Register events - Listener Only
		this.getServer().getPluginManager().registerEvents( this, this );
	}
	
	@Override
	public void onDisable()
	{
		//disable shit
		this._dao.disconnect(this.getDataFolder());
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent e)
	{
		final String playerName = e.getPlayer().getName();
		final Player target = e.getPlayer();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable(){
			@Override
			public void run(){
				String nickname = _dao.getPendingNickname(playerName);
				if (!nickname.equals("off"))
				{
					System.out.println("Player has pending nick!  Applying...");
					final String commandString = "nick " + playerName + " " + nickname;
					
					// Run command a second later, after player has properly joined.
					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask( plugin , new Runnable() {
			            public void run()
			            {
			            	if ( target != null )
			            	{
			            		System.out.println("Running command: " + commandString);
			            		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandString);
			            	}
			            }
			        }, 20L );
					
					_dao.removeFromQueue(playerName);
				}
			}
		});
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) 
	{
		String cmdname = command.getName().toLowerCase();

	    if (cmdname.equals("nickreq") && args.length == 0) 
	    {
	    	sender.sendMessage(ChatColor.GREEN + "To request a nickname type: /nickreq request DesiredNickname");
	    	if (sender.hasPermission("nickreq.setnicks") || sender.isOp())
	    	{
	    		sender.sendMessage(ChatColor.GREEN + "To view nicks type: /nickreq check (page)");
	    		sender.sendMessage(ChatColor.GREEN + "To approve nicks type: /nickreq approve id");
	    	}
	    	return true;
	    }
	    else if (cmdname.equals("nickreq") && args.length > 0)
	    {
	    	String cmd = args[0].toLowerCase();
	    	System.out.println("args[0]: " + args[0]);
	    	if (cmd.equals("request"))
	    	{
	    		System.out.println("cmd matched request");
	    		if (args.length < 2)
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "usage: /nickreq request DesiredNick");
	    			return true;
	    		}
	    		else if (args.length > 2)
	    		{
	    			sender.sendMessage("too many arguments!");
	    			return true;
	    		}
	    		else  //args.length == 2
	    		{
	    			if(!sender.hasPermission("nickreq.nick")  && !sender.isOp())
	    			{
	    				sender.sendMessage(ChatColor.GREEN + "You do not have permission to have a nickname!");
	    				sender.sendMessage(ChatColor.GREEN + "Visit " + Color.CYAN + _website);
	    				return true;
	    			}
	    			else
	    			{
	    				String desiredNick = args[1];
	    				CharSequence ampersandCheck = "&";
	    				boolean containsColors = desiredNick.contains(ampersandCheck);
	    				
	    				if (containsColors && !sender.hasPermission("nickreq.color") && !sender.isOp())
	    				{
	    					sender.sendMessage(ChatColor.GREEN + "You do not have permission to have colors in your nickname!");
	    					sender.sendMessage(ChatColor.GREEN + "visit " + Color.CYAN + _website);
	    					return true;
	    				}
	    				else if (desiredNick.indexOf("&k") != -1 ||
	    						desiredNick.indexOf("&l") != -1 ||
	    						desiredNick.indexOf("&n") != -1 ||
	    						desiredNick.indexOf("&o") != -1)
	    				{
	    					sender.sendMessage(ChatColor.GREEN + "Nicknames may not contain &k, &l, &n, or &o");
	    					return true;
	    				} else if (onBlackList(desiredNick) != null)
	    				{
	    					String word = onBlackList(desiredNick);
	    					sender.sendMessage(ChatColor.GREEN + "Your nickname contains the following disallowed word: " + Color.CYAN + word);
	    					sender.sendMessage(ChatColor.GREEN + "Please request a different nickname.");
	    					return true;
	    				} else {
	    					int count = StringUtils.countMatches(desiredNick, "&");
	    					int length = desiredNick.length() - (count*2);
	    					if (length > 20)
	    					{
	    						sender.sendMessage(ChatColor.GREEN + "Your requested nickname must be 20 characters or fewer!");
	    						return true;
	    					}
	    					boolean previous = _dao.addReq(sender.getName(), desiredNick);
	    					if (!previous)
	    					{
	    						String colored = ChatColor.translateAlternateColorCodes('&', desiredNick);
	    						sender.sendMessage(ChatColor.GREEN + "Your nickname request: " + colored + ChatColor.GREEN + " has been added to the queue!");
	    						sender.sendMessage(ChatColor.GREEN + "Please wait for a staff member to approve it!");

                                sendToMods(ChatColor.GREEN + "New Nickname Request!");

	    					} else
	    					{
	    						String colored = ChatColor.translateAlternateColorCodes('&', desiredNick);
	    						sender.sendMessage(ChatColor.GREEN + "Your new nickname request: " + colored + ChatColor.GREEN + " has been added to the queue");
	    						sender.sendMessage(ChatColor.GREEN + "and has replaced your previous nickname request. ");
	    						sender.sendMessage(ChatColor.GREEN + "Please wait for a staff member to approve it!");

                                sendToMods(ChatColor.GREEN + "New Nickname Request!");

	    					}
	    					
	    					return true;
	    				}
	    			}
	    		}
	    	}
	    	else if (cmd.equals("check"))
	    	{
	    		System.out.println("cmd equaled check");
	    		if (!sender.hasPermission("nickreq.setnicks") && !sender.isOp())
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "This command is for staff only");
	    		} else
	    		{
	    			String pageNumber = "1";
	    			if (args.length == 1)
	    			{
	    				pageNumber = "1";
	    				System.out.println("[NickReq] assuming nickreq check page is 1");
	    			}
	    			else if (args.length == 2)
	    			{
	    				pageNumber = args[1];	
	    			} 
	    			else
	    			{
	    				sender.sendMessage(ChatColor.GREEN + "Too many arguments!");
	    				return true;
	    			}
	    			int pageNumberInt = 1;
	    			try {
	    				pageNumberInt = Integer.parseInt(pageNumber);
	    			} catch (NumberFormatException e) {
	    				sender.sendMessage(ChatColor.GREEN + "Page to check must be a number!");
	    				return true;
	    			}
	    			List<String> resultReqs = _dao.getReqs();
	    			sender.sendMessage(ChatColor.GREEN + "------- NickReqs Page " + pageNumberInt + " -------");
	    			sender.sendMessage(ChatColor.GREEN + "ID# // User // Nickname");
	    			int i = 0;
	    			for (String item : resultReqs)
	    			{
	    				if (i < (pageNumberInt - 1)*5)
	    				{
	    					i++;
	    				} 
	    				else if (i > (pageNumberInt-1)*5 + 5)
	    				{
	    					i++;
	    				}
	    				else {
	    					String colored = ChatColor.translateAlternateColorCodes('&', item);
	    					sender.sendMessage(colored);
	    					i++;
	    				}
	    			}
	    			sender.sendMessage(ChatColor.GREEN + "-----------------------------");
	    			return true;
	    			
	    		}
	    	}
	    	else if (cmd.equals("approve"))
	    	{
	    		if (!sender.hasPermission("nickreq.setnicks") && !sender.isOp())
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "This command is for staff only!");
	    			return true;
	    		}
	    		else if (args.length > 2)
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "Too many arguments!");
	    			return true;
	    		}
	    		else if (args.length == 1)
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "Please enter an ID# to approve");
	    		} 
	    		else
	    		{
	    			int numToApprove = 0;
	    			try {
	    				numToApprove = Integer.parseInt(args[1]);
	    			} catch (NumberFormatException e) {
	    				sender.sendMessage(ChatColor.GREEN + "ID to approve must be a number!");
	    				return true;
	    			}
	    			boolean success = false;
	    			
	    			success = _dao.approveNick(numToApprove, sender.getName(), sender);
	    			
	    			if (success)
	    			{
						sendToMods(ChatColor.GREEN + "Nick #" + numToApprove + ChatColor.GREEN + " has been approved by " + sender.getName());
						return true;
	    			}
	    			else
	    			{
	    				sender.sendMessage(ChatColor.GREEN + "Could not find ID# " + numToApprove);
	    			}
	    			return true;
	    			
	    		}
	    	}
	    	else if (cmd.equals("deny"))
	    	{
	    		if (!sender.hasPermission("nickreq.setnicks") && !sender.isOp())
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "This command is for staff only!");
	    			return true;
	    		}
	    		else if (args.length > 2)
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "Too many arguments!");
	    			return true;
	    		}
	    		else if (args.length == 1)
	    		{
	    			sender.sendMessage(ChatColor.GREEN + "Please enter an ID# to deny");
	    			return true;
	    		} 
	    		else
	    		{
	    			int numToDeny = 0;
	    			try {
	    				numToDeny = Integer.parseInt(args[1]);
	    			} catch (NumberFormatException e) {
	    				sender.sendMessage(ChatColor.GREEN + "ID to deny must be a number!");
	    				return true;
	    			}
	    			boolean success = _dao.denyNick(numToDeny, sender.getName());
	    			if (success)
	    			{
						sendToMods(ChatColor.GREEN + "Nick #" + numToDeny + ChatColor.GREEN + " has been denied by " + sender.getName());
						
						return true;
	    			}
	    			else
	    			{
	    				sender.sendMessage(ChatColor.GREEN + "Could not find ID# " + numToDeny);
	    				return true;
	    			}
	    			
	    		}
	    	}
	    	else
	    	{
	    		sender.sendMessage(ChatColor.GREEN + "[NickReq] unknown command");
	    		return true;
	    	}
	    }
	    
	    return false;
	    
	}
	
	String onBlackList(String nickname)
	{

		for(String item : this._blacklist)
		{
			if (nickname.toLowerCase().contains(item.toLowerCase())) return item;
		}
		
		return null;
	}

    void sendToMods(String message)
    {

        for(Player player: getServer().getOnlinePlayers())
        {
            if(player.hasPermission("nickreq.setnicks") || player.isOp())
            {
                player.sendMessage(message);
            }
        }

    }

}
