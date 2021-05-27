package com.viral32111.sethome;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;

// https://papermc.io/javadocs/paper/1.16/io/papermc/paper/event/player/PlayerDeepSleepEvent.html
// https://papermc.io/javadocs/paper/1.16/org/bukkit/event/player/PlayerBedEnterEvent.html

public class SetHomeListener implements Listener {

	@EventHandler
	public void onPlayerBedEnter( PlayerBedEnterEvent event ) {
		System.out.println( event.getPlayer().displayName().toString() + " attempting to sleep in bed [" + event.getBed().getX() + ", " + event.getBed().getY() + ", " + event.getBed().getZ() + "] with result " + event.getBedEnterResult() );
	}

}