package com.yd.blockbuster.managers;

import com.yd.blockbuster.models.Selection;
import org.bukkit.Location;

public class SelectionManager {

    private Location pos1;
    private Location pos2;

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean isSelectionComplete() {
        return pos1 != null && pos2 != null;
    }

    public Selection getSelection() {
        if (isSelectionComplete()) {
            return new Selection(pos1, pos2);
        }
        return null;
    }

}
