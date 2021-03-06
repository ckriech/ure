package ure.ui.modals;

import ure.commands.UCommand;
import ure.commands.UCommandMove;
import ure.math.UColor;
import ure.render.URenderer;
import ure.sys.GLKey;

/**
 * ModalDirection queries the user for a cardinal direction (or space for no direction, if allowed).
 *
 */
public class UModalDirection extends UModal {

    String prompt;
    boolean acceptNull;
    boolean escapable;

    int cellx, celly;

    String glyphs = "^>v<";
    UColor glyphColor;

    public UModalDirection(String _prompt, boolean _acceptNull, int cellx, int celly, boolean _escapable, HearModalDirection _callback, String _callbackContext) {
        super(_callback, _callbackContext, null);
        prompt = _prompt;
        acceptNull = _acceptNull;
        escapable = _escapable;
        this.cellx = cellx;
        this.celly = celly;
        setDimensions(3,3);
        glyphColor = new UColor(commander.config.getHiliteColor());
        dismissFrameEnd = 10;
    }

    @Override
    public void setDimensions(int x, int y) {
        cellw = x;
        cellh = y;
        xpos = (cellx - 1) * commander.config.getGlyphWidth();
        ypos = (celly - 1) * commander.config.getGlyphHeight();
    }

    @Override
    public void hearCommand(UCommand command, GLKey k) {
        if (command.id.startsWith("MOVE")) {
            dismiss();
            ((HearModalDirection) callback).hearModalDirection(callbackContext, ((UCommandMove) command).xdir, ((UCommandMove) command).ydir);
        } else if (command.id.equals("PASS") && acceptNull) {
            dismiss();
            ((HearModalDirection) callback).hearModalDirection(callbackContext, 0, 0);
        } else if (command.id.equals("ESC") && escapable) {
            escape();
        }
    }

    @Override
    public void drawFrame(URenderer renderer) {

    }
    @Override
    public void drawContent(URenderer renderer) {
        commander.printScroll(prompt);
        int gw = commander.config.getGlyphWidth();
        int gh = commander.config.getGlyphHeight();
        renderer.drawGlyph(glyphs.charAt(0), xpos + gw, ypos, glyphColor, 0, 0);
        renderer.drawGlyph(glyphs.charAt(2), xpos + gw, ypos + gh*2, glyphColor, 0, 0);
        renderer.drawGlyph(glyphs.charAt(1), xpos + gw * 2, ypos + gh, glyphColor, 0, 0);
        renderer.drawGlyph(glyphs.charAt(3), xpos, ypos + gh, glyphColor, 0, 0);
    }

    @Override
    public void animationTick() {
        if (dismissed) {
            glyphColor.setAlpha((float)Math.sin(commander.frameCounter * 2f) * 0.5f + 0.6f);
        } else {
            glyphColor.setAlpha((float) Math.sin(commander.frameCounter * 0.14f) * 0.3f + 0.4f);
        }
        super.animationTick();
    }
}
