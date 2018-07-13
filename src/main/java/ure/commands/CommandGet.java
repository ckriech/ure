package ure.commands;

import ure.actions.ActionGet;
import ure.actors.UPlayer;

public class CommandGet extends UCommand {

    public static String id = "GET";

    @Override
    public void execute(UPlayer player) {
        player.doAction(new ActionGet(player));
    }
}