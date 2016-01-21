package me.markeh.factionsframework.faction;

import java.util.List;

import me.markeh.factionsframework.factionsmanager.FactionsManager;
import me.markeh.factionsframework.objs.Loc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

// Use FactionsManager.get().fetch() to get this
public abstract class Factions {
	
	private static Factions factions = null;
	public static Factions get() {
		if (factions == null) factions = FactionsManager.get().fetch();
		
		return factions;
	}
	
	// Get Faction by their ID
	public abstract Faction getFactionById(String id);
	
	// Get Faction at a location
	public abstract Faction getFactionAt(Location location);
	
	public Faction getFactionAt(Loc location) {
		return this.getFactionAt(location.asBukkitLocation());
	}
		
	// Get Faction for a player
	public abstract Faction getFactionFor(Player player);
	
	// Get Wilderness ID
	public abstract String getWildernessId();
	
	// Get Safezone ID
	public abstract String getSafeZoneId();
		
	// Get WarZone ID
	public abstract String getWarZoneId();
	
	// Check if factions is enabeld 
	public abstract boolean isFactionsEnabled(World world);
	
	public abstract List<Faction> getAll();
	
	/* Deprecated Rewrites */
	
	// EOL: 4 Releases after 2.0.0-beta4
	@Deprecated
	public String getWildernessID() { return this.getWildernessId(); }
	
	// EOL: 4 Releases after 2.0.0-beta4
	@Deprecated
	public String getSafezoneID()  { return this.getSafeZoneId(); }
		
	// EOL: 4 Releases after 2.0.0-beta4
	@Deprecated
	public String getWarZoneID()  { return this.getWarZoneId(); }
	
	// EOL: 4 Releases after 2.0.0-beta4
	@Deprecated
	public Faction getFactionByID(String id) { return this.getFactionById(id); }

}
