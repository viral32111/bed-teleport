package com.viral32111.sethome;

/*
AUTHOR: Dev_Richard (https://www.spigotmc.org/members/dev_richard.38792/)
DESC: A simple and easy to use class that can get and set a player's total experience points.

Feel free to use this class in both public and private plugins, however if you release your
plugin please link to this gist publicly so that others can contribute and benefit from it.

https://gist.github.com/RichardB122/8958201b54d90afbc6f0
*/

import java.math.BigDecimal;
import org.bukkit.entity.Player;

public class ExperienceManager {

	private final Player player;

	public ExperienceManager(Player player) {
		this.player = player;
	}

	private float round(float d) {
		BigDecimal bd = new BigDecimal(Float.toString(d));
		bd = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN);
		return bd.floatValue();
	}

	public int getTotalExperience() {
		int experience;
		int level = player.getLevel();
		if(level >= 0 && level <= 15) {
			experience = (int) Math.ceil(Math.pow(level, 2) + (6 * level));
			int requiredExperience = 2 * level + 7;
			double currentExp = Double.parseDouble(Float.toString(player.getExp()));
			experience += Math.ceil(currentExp * requiredExperience);
			return experience;
		} else if(level > 15 && level <= 30) {
			experience = (int) Math.ceil((2.5 * Math.pow(level, 2) - (40.5 * level) + 360));
			int requiredExperience = 5 * level - 38;
			double currentExp = Double.parseDouble(Float.toString(player.getExp()));
			experience += Math.ceil(currentExp * requiredExperience);
			return experience;
		} else {
			experience = (int) Math.ceil(((4.5 * Math.pow(level, 2) - (162.5 * level) + 2220)));
			int requiredExperience = 9 * level - 158;
			double currentExp = Double.parseDouble(Float.toString(player.getExp()));
			experience += Math.ceil(currentExp * requiredExperience);
			return experience;
		}
	}

	public void setTotalExperience(int xp) {
		// Levels 0 through 15
		if (xp >= 0 && xp < 351) {
			int a = 1; int b = 6; int c = -xp;
			int level = (int) (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
			int xpForLevel = (int) (Math.pow(level, 2) + (6 * level));
			int remainder = xp - xpForLevel;
			int experienceNeeded = (2 * level) + 7;
			float experience = (float) remainder / (float) experienceNeeded;
			experience = round(experience);
			player.setLevel(level);
			player.setExp(experience);

		// Levels 16 through 30
		} else if (xp >= 352 && xp < 1507) {
			double a = 2.5; double b = -40.5; int c = -xp + 360;
			double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
			int level = (int) Math.floor(dLevel);
			int xpForLevel = (int) (2.5 * Math.pow(level, 2) - (40.5 * level) + 360);
			int remainder = xp - xpForLevel;
			int experienceNeeded = (5 * level) - 38;
			float experience = (float) remainder / (float) experienceNeeded;
			experience = round(experience);
			player.setLevel(level);
			player.setExp(experience);

		// Level 31 and greater
		} else {
			double a = 4.5; double b = -162.5; int c = -xp + 2220;
			double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
			int level = (int) Math.floor(dLevel);
			int xpForLevel = (int) (4.5 * Math.pow(level, 2) - (162.5 * level) + 2220);
			int remainder = xp - xpForLevel;
			int experienceNeeded = (9 * level) - 158;
			float experience = (float) remainder / (float) experienceNeeded;
			experience = round(experience);
			player.setLevel(level);
			player.setExp(experience);
		}
	}
}
