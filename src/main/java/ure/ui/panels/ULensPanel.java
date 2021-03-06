package ure.ui.panels;

import ure.actors.UActor;
import ure.areas.UCell;
import ure.math.UColor;
import ure.render.URenderer;
import ure.sys.UConfig;
import ure.terrain.UTerrain;
import ure.things.UThing;
import ure.ui.UCamera;

public class ULensPanel extends UPanel {

    UCamera camera;
    int cameraX,cameraY;
    int cellw,cellh;
    int xpos,ypos;

    int updateFrame = 0;

    public ULensPanel(UCamera watchCamera, int cameraOffsetX, int cameraOffsetY, int _pixelw, int _pixelh, int _padx, int _pady, UColor _fgColor, UColor _bgColor, UColor _borderColor) {
        super(_pixelw,_pixelh,_padx,_pady,_fgColor,_bgColor,_borderColor);
        camera = watchCamera;
        cameraX = cameraOffsetX;
        cameraY = cameraOffsetY;
        setCameraOffsets();
    }

    public void setCameraOffsets() {
        int screenw = 0, screenh = 0;
        if (commander.config.getModalPosition() == UConfig.POS_WINDOW_CENTER) {
            screenw = commander.config.getScreenWidth();
            screenh = commander.config.getScreenHeight();
        } else {
            screenw = camera.getWidthInCells() * commander.config.getGlyphWidth();
            screenh = camera.getHeightInCells() * commander.config.getGlyphHeight();
        }

        xpos = (screenw - (cellw * commander.config.getGlyphWidth())) / 2;
        ypos = (screenh - (cellh * commander.config.getGlyphHeight())) / 2;
    }

    @Override
    public void draw(URenderer renderer) {
        if (!hidden) {
            renderer.drawRectBorder(1, 1, width - 2, height - 2, 1, bgColor, borderColor);
            int mousex = commander.mouseX();
            int mousey = commander.mouseY();
            int x = toCellX(mousex);
            int y = toCellY(mousey);
            if (camera.visibilityAt(x, y) < commander.config.getVisibilityThreshold())
                return;
            UTerrain t = camera.terrainAt(x, y);
            if (t != null) {
                renderer.drawString(xpos + padX + renderer.glyphWidth() * 2, ypos + padY, commander.config.getTextColor(), t.getName());
                t.getIcon().draw(renderer, xpos + padX, ypos + padY);
            }
            UCell cell = camera.area.cellAt(x + camera.leftEdge, y + camera.topEdge);
            if (cell != null) {
                UThing thing = cell.topThingAt();
                if (thing != null) {
                    thing.getIcon().draw(renderer, xpos + padX, ypos + padY + renderer.glyphHeight());
                    renderer.drawString(xpos + padX + renderer.glyphWidth() * 2, ypos + padY + renderer.glyphHeight(), commander.config.getTextColor(), thing.getIname());

                }
                UActor actor = cell.actorAt();
                if (actor != null) {
                    actor.icon().draw(renderer, xpos + padX, ypos + padY + renderer.glyphHeight() * 2);
                    renderer.drawString(xpos + padX + renderer.glyphWidth() * 2, ypos + padY + commander.config.getGlyphHeight() * 2, commander.config.getTextColor(), actor.getIname());
                }
            }
        }
    }

    public int toCellX(int x) {
        return (x - cameraX) / commander.config.getGlyphWidth();
    }
    public int toCellY(int y) {
        return (y - cameraY) / commander.config.getGlyphHeight();
    }
}
