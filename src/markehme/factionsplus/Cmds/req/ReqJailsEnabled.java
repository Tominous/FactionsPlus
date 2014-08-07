package markehme.factionsplus.Cmds.req;

import markehme.factionsplus.MCore.FPUConf;

import org.bukkit.command.CommandSender;

import com.massivecraft.factions.entity.UPlayer;
import com.massivecraft.massivecore.cmd.MassiveCommand;
import com.massivecraft.massivecore.cmd.req.ReqAbstract;

public class ReqJailsEnabled extends ReqAbstract {
	private static final long serialVersionUID = 1L;

	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static ReqJailsEnabled i = new ReqJailsEnabled();
	public static ReqJailsEnabled get() { return i; }

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public boolean apply(CommandSender sender, MassiveCommand command) {
		return FPUConf.get(UPlayer.get(sender).getUniverse()).jailsEnabled;
	}

	@Override
	public String createErrorMessage(CommandSender sender, MassiveCommand command) {
		return "Jails are not enabled.";
	}
}
