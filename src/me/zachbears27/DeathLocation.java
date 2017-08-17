package me.zachbears27;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;

import me.zachbears27.MySQL;
import net.md_5.bungee.api.ChatColor;

// This Plugin Is Made For MumboCraft & It's Players & Staff 
// Any misuse or re-distribution of this plugin will call for said plugin to be removed
// Written by A_Brave_Panda & Jamdoggy

public class DeathLocation extends JavaPlugin implements Listener {
	
	private MySQL MySQLC = null;
	private Connection c = null;

	// Retrieve MySQL database connection details from the config.yml file
	String host     = getConfig().getString("Hostname");
	String port     = getConfig().getString("Port");
	String db       = getConfig().getString("Database");
	String username = getConfig().getString("Username");
	String password = getConfig().getString("Password");
	String table    = getConfig().getString("Table");
			
 
	@Override
	public void onDisable() {
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		registerConfig();
		
		this.MySQLC = new MySQL(host, port, db, username, password);
		try {
			this.c = this.MySQLC.openConnection();
		} catch (ClassNotFoundException er) {
			er.printStackTrace();
		} catch (SQLException er) {
			er.printStackTrace();
		}
	}
 
	public void registerConfig() {
		saveDefaultConfig();
	}

 	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {

		if (e.getEntity() instanceof Player) {
			Player p = e.getEntity();
			Location loc = e.getEntity().getLocation();
			double x = loc.getBlockX() + 0.5;
			double y = loc.getBlockY();
			double z = loc.getBlockZ() + 0.5;
			String dr = e.getDeathMessage();
			String world = loc.getWorld().getName();
			java.util.Date dt = new java.util.Date();

			java.text.SimpleDateFormat sdf = 
					new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			String time = sdf.format(dt);
			inventoryContents.put(p.getName(), p.getInventory().getContents());
			inventoryArmorContents.put(p.getName(), p.getInventory().getArmorContents());
      
			String statementstring = "SELECT * FROM " + table + " WHERE PlayerName = '" + p.getName() +"';";
			String statementstring2 = "INSERT INTO "+ table + " (id, PlayerName, X, Y, Z, DeathReason, world,Time) VALUES (NULL, '" + p.getName() + "', '" + x + "', '" + y + "', '" + z + "', '" + dr + "', '" + world + "', '" + time + "');";
			
			this.MySQLC = new MySQL(host, port, db, username, password);
			try {
				this.c = this.MySQLC.openConnection();
			} catch (ClassNotFoundException er) {
				er.printStackTrace();
			} catch (SQLException er) {
				er.printStackTrace();
			}
		
			try {
				Statement statement = c.createStatement();
				ResultSet res = statement.executeQuery(statementstring);
				if(res.next()) {  // Player already has a death in the database, update it
					String statementstring3 = "UPDATE " + table + " SET X='" + x + "', Y='" + y + "', Z='" + z + "', DeathReason='" + dr + "', world='" + world + "',Time='" + time + "' WHERE id='" + res.getInt("id") + "';";
					p.sendMessage(statementstring3); 
					statement.executeUpdate(statementstring3);
				} else {         // First time player has died, create a new death entry
					statement.executeUpdate(statementstring2);
				}
			} catch (SQLException e2) {
				e2.printStackTrace();
			}
		}

	}
  
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = (Player)sender;
		boolean tp = false;
		boolean load = false;
		String statementstring4 = null;
		if(cmd.getLabel().equalsIgnoreCase("death")) {

			this.MySQLC = new MySQL(host, port, db, username, password);
			try {
				this.c = this.MySQLC.openConnection();
			} catch (ClassNotFoundException er) {
				er.printStackTrace();
			} catch (SQLException er) {
				er.printStackTrace();
			}

			if(args.length != 0) {
				if(args[0].equals("help")) {
					p.sendMessage(ChatColor.YELLOW + "/death" + ChatColor.GOLD + "      : Displays the co-ordinates of your last death");
					p.sendMessage(ChatColor.YELLOW + "/death help" + ChatColor.GOLD + " : Displays this information page");
					if(p.hasPermission("death.others")) {	
						p.sendMessage(ChatColor.YELLOW + "/death <player>" + ChatColor.GOLD + "     : Displays the co-ordinates of players last death");
					}
					if(p.hasPermission("death.teleport")) {	
						p.sendMessage(ChatColor.YELLOW + "/death tp <player>" + ChatColor.GOLD + "  : Teleport to players last death location");
					}
				} else if(args[0].equals("tp")) {
					if(p.hasPermission("death.teleport")) {	
						String sanitised_arg = args[1].replace("\\","");
						sanitised_arg = sanitised_arg.replace("'","");
						statementstring4 = "SELECT * FROM " + table + " WHERE PlayerName = '" + sanitised_arg.toString() +"';";
						try {
							Statement statement = c.createStatement();
							ResultSet res = statement.executeQuery(statementstring4);
							if(res.next()) {
								Location l = new Location(Bukkit.getWorld(res.getString("world")), res.getDouble("X"), res.getInt("Y"), res.getDouble("Z"));
								p.teleport(l);
								p.sendMessage(ChatColor.GRAY +  "You have been teleported to " + ChatColor.GOLD + "" + ChatColor.BOLD + res.getString("PlayerName") + ChatColor.GRAY + "'s death location at [" +  ChatColor.GOLD + "" +
										res.getString("world") + ": " + res.getString("X") + ", " + res.getString("Y") + ", " + res.getString("Z") + ChatColor.GRAY + "] at: " + ChatColor.GOLD + "" + 
										res.getString("time") + ChatColor.GRAY + ".");
								p.sendMessage(ChatColor.GRAY + "Cause of Death: " + ChatColor.GOLD + res.getString("DeathReason") + ChatColor.GRAY + "." );
							} else {
								p.sendMessage(ChatColor.RED + args[1] + " hasn't died! (yet :D)");
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					} else {
						p.sendMessage(ChatColor.RED + "You do not have permission to teleport to your death location.");
					}
					tp = true;
				} else if (p.hasPermission("death.others")) {
					// Player used /death <name> and has permission to do so
					String sanitised_arg = args[0].replace("\\","");
					sanitised_arg = sanitised_arg.replace("'","");
					statementstring4 = "SELECT * FROM " + table + " WHERE PlayerName = '" + sanitised_arg +"';";
				} else {
					// Player used /death <name> but doesn't have permission to do so
					statementstring4 = "SELECT * FROM " + table + " WHERE PlayerName = '" + p.getName() +"';";
				}
			} else {
				// Player just used /death with no args - use their own name
				statementstring4 = "SELECT * FROM " + table + " WHERE PlayerName = '" + p.getName() +"';";
			}
	  
			// If 'tp' wasn't used...
			if (tp == false || load == false) {
				try {
					Statement statement = c.createStatement();
					ResultSet res = statement.executeQuery(statementstring4);
					if(res.next()) {
						p.sendMessage(ChatColor.GRAY +  "Player " + ChatColor.GOLD + "" + ChatColor.BOLD + res.getString("PlayerName") + ChatColor.GRAY + " died at [" +  ChatColor.GOLD + "" +
								res.getString("world") + ": " + res.getString("X") + ", " + res.getString("Y") + ", " + res.getString("Z") + ChatColor.GRAY + "] at: " + ChatColor.GOLD + "" + 
								res.getString("time") + ChatColor.GRAY + ".");
						p.sendMessage(ChatColor.GRAY + "Cause of Death: " + ChatColor.GOLD + res.getString("DeathReason") + ChatColor.GRAY + "." );
					} else if(args.length != 0) {
						p.sendMessage(ChatColor.RED + args[0] + " hasn't died! (yet :D)");
					} else {
						p.sendMessage(ChatColor.RED + " You haven't died! (yet :D)");
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}	
		}  // End command: death
		return true;
	}
}