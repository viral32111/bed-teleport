// The package this file is a part of
package com.viral32111.sethome;

// Import required classes
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// The main entry point class
public class SetHome extends JavaPlugin {

	// Globals for config values
	boolean isExperienceEnabled;
	boolean isExperienceScaleEnabled;
	int experienceAmount;
	double experienceScaleFactor;

	// This event runs whenever the plugin is loaded
	@Override
	public void onEnable() {

		// Copy the default configuration
		saveDefaultConfig();

		// Get configuration values
		ConfigurationSection experienceConfig = getConfig().getConfigurationSection( "experience" );
		ConfigurationSection experienceScaleConfig = experienceConfig.getConfigurationSection( "scale" );
		isExperienceEnabled = experienceConfig.getBoolean( "enabled" );
		isExperienceScaleEnabled = experienceScaleConfig.getBoolean( "enabled" );
		experienceAmount = experienceConfig.getInt( "amount" );
		experienceScaleFactor = experienceScaleConfig.getDouble( "factor" );

	}

	// This event runs whenever a chat command is executed
	@Override
	public boolean onCommand( @NotNull CommandSender sender, @NotNull Command command, @NotNull String name, String[] arguments ) {

		// Do not continue if the console ran the command
		if ( !( sender instanceof Player ) ) {
			getLogger().info( "This command is not usable by the console." );
			return false;
		}

		// Cast sender to player
		Player player = ( Player ) sender;

		// Is this the /bed command?
		if ( command.getName().equalsIgnoreCase( "bed" ) && player.hasPermission( "sethome.bed" ) ) {

			// Don't continue if the player isn't in the Overworld
			if ( player.getWorld().getEnvironment() != World.Environment.NORMAL ) {
				player.sendMessage( "You can only return to your bed when in the Overworld." );
				return true;
			}

			// Get the player's bed spawn location
			Location respawnPointLocation = player.getBedSpawnLocation();

			// Don't continue if their bed is missing or obstructed
			if ( respawnPointLocation == null ) {
				player.sendMessage( "You have no bed or it is obstructed!" );
				getLogger().info( player.getName() + " attempted to teleport back to their bed, but it is missing or obstructed." );
				return true;
			}

			// If experience is enabled, don't continue if the player doesn't have enough experience, if they do then subtract it
			if ( isExperienceEnabled ) {
				int experience = player.getLevel() ^ 2 + 6 * player.getLevel(); // https://minecraft.fandom.com/wiki/Experience#Leveling_up

				ExperienceManager playerExperience = new ExperienceManager( player );
				System.out.println( playerExperience.getTotalExperience() );

				if ( isExperienceScaleEnabled ) {
					double distanceToBed = respawnPointLocation.distance( player.getLocation() );
					System.out.println( distanceToBed );

					if ( playerExperience.getTotalExperience() < ( distanceToBed * experienceScaleFactor ) ) {
						player.sendMessage( "You do not have enough experience to teleport back to your bed!" );
						getLogger().info( player.getName() + " attempted to teleport back to their bed, but they do not have enough experience." );
						return true;
					} else {
						// Subtract experience here
						player.playSound( player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f );
					}
				} else {
					if ( playerExperience.getTotalExperience() < experienceAmount ) {
						player.sendMessage( "You do not have enough experience to teleport back to your bed!" );
						getLogger().info( player.getName() + " attempted to teleport back to their bed, but they do not have enough experience." );
						return true;
					} else {
						player.giveExp( -experienceAmount );
						player.playSound( player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f );
					}
				}
			}

			// Teleport the player to their bed
			player.teleport( respawnPointLocation );
			player.sendMessage( "You have teleported back to your bed." );
			getLogger().info( player.getName() + " has teleported back to their bed at [ " + respawnPointLocation.getX() + ", " + respawnPointLocation.getY() + ", " + respawnPointLocation.getZ() + " ]" );

			return true;

		}

		/* Code respawn anchor functionality for a future version
		if ( command.getName().equalsIgnoreCase( "anchor" ) && player.hasPermission( "sethome.anchor" ) ) {

			if ( player.getWorld().getEnvironment() != World.Environment.NETHER ) {
				player.sendMessage( "You can only return to your respawn anchor when in the Nether." );
				return true;
			}

			Location respawnAnchorLocation = player. GET RESPAWN ANCHOR LOCATION ??

			if ( respawnAnchorLocation == null ) { // Check if it is charged here too
				player.sendMessage( "You have no charged respawn anchor!" );
				getLogger().info( player.getName() + " attempted to teleport back to their respawn anchor, but it is missing, obstructed or not charged." );
				return true;
			}

			player.teleport( respawnAnchorLocation );
			player.sendMessage( "You have teleported back to your respawn anchor." );
			getLogger().info( player.getName() + " has teleported back to their respawn anchor at " + respawnAnchorLocation.toString() );

			return true;

		}
		*/

		return false;
	}

}
