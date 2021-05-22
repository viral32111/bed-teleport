// The package this file is a part of
package com.viral32111.sethome;

// Import syntax annotations
import org.jetbrains.annotations.NotNull;

// Import the base Bukkit plugin class
import org.bukkit.plugin.java.JavaPlugin;

// Import chat command classes
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

// Import player class
import org.bukkit.entity.Player;

// The main entry point class
public class SetHome extends JavaPlugin {

	// This event runs whenever the plugin is loaded
	@Override
	public void onEnable() {

		// Copy the default configuration into the plugin's folder
		saveDefaultConfig();

		// Print a startup message in the console
		getLogger().info( "Successfully loaded!" );

	}

	// This event runs whenever the plugin is unloaded
	@Override
	public void onDisable() {

		// Print a shutdown message in the console
		getLogger().info( "Goodbye." );

	}

	// This event runs whenever a chat command is executed
	@Override
	public boolean onCommand( @NotNull CommandSender sender, @NotNull Command command, @NotNull String name, String[] arguments ) {

		// Do not continue if the console ran the command
		if ( !( sender instanceof Player ) ) {
			getLogger().info( "This command is not usable by the console." );
			return false;
		}

		Player player = ( Player ) sender;

		if ( command.getName().equalsIgnoreCase( "sethome" ) && player.hasPermission( "sethome.create" ) ) {
			if ( arguments.length <= 0 ) {
				player.sendMessage( "You must specify a name for the home point." );
				return false;
			}

			player.sendMessage( "Creating home point " + arguments[ 0 ] + "..." );
			return true;
		}

		if ( command.getName().equalsIgnoreCase( "delhome" ) && player.hasPermission( "sethome.delete" ) ) {
			if ( arguments.length <= 0 ) {
				player.sendMessage( "You must specify a name for the home point." );
				return false;
			}

			player.sendMessage( "Deleting a home point " + arguments[ 0 ] + "..." );
			return true;
		}

		if ( command.getName().equalsIgnoreCase( "home" ) && player.hasPermission( "sethome.teleport" ) ) {
			if ( arguments.length <= 0 ) {
				player.sendMessage( "You must specify a name for the home point." );
				return false;
			}

			player.sendMessage( "Teleporting to home point " + arguments[ 0 ] + "..." );
			return true;
		}

		if ( command.getName().equalsIgnoreCase( "bed" ) && player.hasPermission( "sethome.bed" ) ) {
			player.sendMessage( "Teleporting to your bed..." );
			return true;
		}

		if ( command.getName().equalsIgnoreCase( "anchor" ) && player.hasPermission( "sethome.anchor" ) ) {
			player.sendMessage( "Teleporting to your respawn anchor..." );
			return true;
		}

		return false;
	}

}
