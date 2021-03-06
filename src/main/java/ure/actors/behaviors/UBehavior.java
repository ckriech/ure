package ure.actors.behaviors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ure.actors.actions.ActionWalk;
import ure.actors.UActor;
import ure.math.UColor;
import ure.math.UPath;
import ure.sys.Entity;
import ure.sys.Injector;
import ure.sys.UCommander;
import ure.actors.actions.UAction;
import ure.actors.UNPC;
import ure.things.UThing;

import javax.inject.Inject;

/**
 * UBehavior implements a source of actions for an NPC actor to perform, to emulate a behavior or achieve
 * a goal.  Subclass UBehavior to create behavior styles and patterns for your custom NPCs.
 *
 * A UBehavior is spawned and attached to a new NPC, and can maintain state about the NPC to help it
 * make decisions.
 *
 */
public abstract class UBehavior {

    @Inject
    @JsonIgnore
    UCommander commander;

    public UActor actor;        // the actor we're a part of

    protected String TYPE = "";

    float relativeUrgency;      // how urgent are we vs our NPC's other behaviors?
    float currentUrgency;       // urgency of our last action request
    String currentStatus;       // english status for UI, based on last action
    UColor currentStatusColor;

    public UBehavior() {
        Injector.getAppComponent().inject(this);
        currentUrgency = 0f;
        currentStatus = "";
        currentStatusColor = UColor.COLOR_GRAY;
    }
    public UBehavior(String type) {
        this();
        this.TYPE = type;
    }

    /**
     * Override action() to return UActions on demand for the actor to perform.
     *
     * @param actor
     * @return
     */
    public UAction action(UNPC actor) {
        return null;
    }

    /**
     * Do we care about perceiving this entity?
     */
    public boolean caresAbout(UNPC actor, Entity entity) {
        return false;
    }

    /**
     * Are we hostile to this entity?
     *
     */
    public boolean isHostileTo(UNPC actor, Entity entity) {
        return false;
    }

    /**
     * React to an event we saw.
     */
    public void hearEvent(UNPC actor, UAction action) {

    }

    /**
     * Will I let actor Interact with me?
     */
    public boolean willInteractWith(UNPC actor, UActor interactor) {
        return false;
    }

    /**
     * Receive an Interact from interactor.
     * Return the actionTime it took.
     */
    public float interactionFrom(UNPC actor, UActor interactor) {
        return 0.5f;
    }

    /**
     * The following utility methods are for use in custom Behavior.action() as shortcuts to certain common
     * responses to situations.
     *
     */

    /**
     * Step toward it.
     */
    public UAction Approach(UNPC actor, Entity entity) {
        int[] step = UPath.nextStep(actor.area(), actor.areaX(), actor.areaY(),
                entity.areaX(), entity.areaY(), actor, 25);
        if (step != null)
            return new ActionWalk(actor, step[0] - actor.areaX(), step[1] - actor.areaY());
        return null;
    }

    /**
     * Go and get it.
     */
    public UAction Get(UNPC actor, UThing thing) {
        return null;
    }

    /**
     * Go and kill it.
     */
    public UAction Attack(UNPC actor, UActor target) {
        if (UPath.mdist(actor.areaX(),actor.areaY(),target.areaX(),target.areaY()) > 2)
            return Approach(actor, target);
        actor.emote(actor + " flails ineffectually.");
        return null;
    }

    /**
     * Get away from it.
     */
    public UAction Avoid(UNPC actor, Entity entity) {
        return null;
    }

    /**
     * Respond to threat from it (by fight or flight).
     */
    public UAction ForF(UNPC actor, UActor threat) {
        return null;
    }

    public float getRelativeUrgency() { return relativeUrgency; }
    public void setRelativeUrgency(float urg) { relativeUrgency = urg; }
    public float getCurrentUrgency() { return currentUrgency; }
    public void setCurrentUrgency(float urg) { currentUrgency = urg; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String status) { currentStatus = status; }
    public UColor getCurrentStatusColor() { return currentStatusColor; }
    public void setCurrentStatusColor(UColor c) { currentStatusColor = c; }
    public UActor getActor() { return actor; }
    public void setActor(UActor _actor) { actor = _actor; }
    public String getTYPE() { return TYPE; }
    public void setTYPE(String t) { TYPE = t; }
}
