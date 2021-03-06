package ure.ui.particles;

import ure.math.UColor;

public class ParticleSpark extends UParticle {

    public ParticleSpark(int _x, int _y, UColor fgColor, int frames, float alpha) {
        super(_x,_y,frames,fgColor,alpha,false);
    }

    @Override
    public char glyph() {
        if (ticksLeft < ticksInitial * 0.4f) {
            return '.';
        } else if (ticksLeft < ticksInitial * 0.6f) {
            return '`';
        } else if (ticksLeft < ticksInitial * 0.8f) {
            return '\'';
        } else {
            return ',';
        }
    }
}
