package com.yd.blockbuster.models;

import org.bukkit.Location;

public class Selection {

    private final Location pos1;
    private final Location pos2;

    public Selection(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) {
            return false;
        }

        double x1 = Math.min(pos1.getX(), pos2.getX());
        double x2 = Math.max(pos1.getX(), pos2.getX());
        double y1 = Math.min(pos1.getY(), pos2.getY());
        double y2 = Math.max(pos1.getY(), pos2.getY());
        double z1 = Math.min(pos1.getZ(), pos2.getZ());
        double z2 = Math.max(pos1.getZ(), pos2.getZ());

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= x1 && x <= x2
                && y >= y1 && y <= y2
                && z >= z1 && z <= z2;
    }

    public Location getCenter() {
        double x = (pos1.getX() + pos2.getX()) / 2;
        double y = (pos1.getY() + pos2.getY()) / 2;
        double z = (pos1.getZ() + pos2.getZ()) / 2;
        return new Location(pos1.getWorld(), x, y, z);
    }
}