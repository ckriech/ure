package ure.actors.behaviors;

import ure.actions.UAction;
import ure.actors.UActor;
import ure.actors.UNPC;
import ure.actors.UPlayer;
import ure.math.UColor;
import ure.sys.Entity;

/**
 * Raar I'm a dumb angry monster!  I see the player and attack her until I get killed.  That's all.
 *
 */
public class BehaviorMonster extends UBehavior {

    public static final String TYPE = "monster";

    public BehaviorMonster() { super(TYPE); }

    @Override
    public UAction action(UNPC actor) {
        for (Entity entity : actor.seenEntities) {
            if (entity instanceof UPlayer) {
                currentUrgency = 1f;
                currentStatus = "hostile";
                currentStatusColor = UColor.COLOR_RED;
                return Attack(actor, (UPlayer)entity);
            }
        }
        currentStatus = "";
        return null;
    }

    @Override
    public boolean caresAbout(UNPC actor, Entity entity) {
        if (entity instanceof UPlayer) {
            System.out.println(actor.getName() + " noticed " + entity.getName() + "...!");
            return true;
        }
        return false;
    }
}
