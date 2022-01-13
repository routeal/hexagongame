// copy from https://www.redblobgames.com/grids/hexagons/implementation.html

package org.nabe.koshigaya;

import androidx.annotation.Nullable;

public class OffsetCoord {

    public OffsetCoord(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public final int col;
    public final int row;
    static public int EVEN = 1;
    static public int ODD = -1;

    static public OffsetCoord qoffsetFromCube(int offset, Hex h) {
        int col = h.q;
        int row = h.r + (int) ((h.q + offset * (h.q & 1)) / 2);
        if (offset != OffsetCoord.EVEN && offset != OffsetCoord.ODD) {
            throw new IllegalArgumentException("offset must be EVEN (+1) or ODD (-1)");
        }
        return new OffsetCoord(col, row);
    }


    static public Hex qoffsetToCube(int offset, OffsetCoord h) {
        int q = h.col;
        int r = h.row - (int) ((h.col + offset * (h.col & 1)) / 2);
        int s = -q - r;
        if (offset != OffsetCoord.EVEN && offset != OffsetCoord.ODD) {
            throw new IllegalArgumentException("offset must be EVEN (+1) or ODD (-1)");
        }
        return new Hex(q, r, s);
    }


    static public OffsetCoord roffsetFromCube(int offset, Hex h) {
        int col = h.q + (int) ((h.r + offset * (h.r & 1)) / 2);
        int row = h.r;
        if (offset != OffsetCoord.EVEN && offset != OffsetCoord.ODD) {
            throw new IllegalArgumentException("offset must be EVEN (+1) or ODD (-1)");
        }
        return new OffsetCoord(col, row);
    }


    static public Hex roffsetToCube(int offset, OffsetCoord h) {
        int q = h.col - (int) ((h.row + offset * (h.row & 1)) / 2);
        int r = h.row;
        int s = -q - r;
        if (offset != OffsetCoord.EVEN && offset != OffsetCoord.ODD) {
            throw new IllegalArgumentException("offset must be EVEN (+1) or ODD (-1)");
        }
        return new Hex(q, r, s);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof OffsetCoord)) {
            return false;
        }

        final OffsetCoord other = (OffsetCoord) obj;

        return this.col == other.col && this.row == other.row;
    }

    @Override
    public int hashCode() {
        Integer ri = new Integer(row);
        Integer rc = new Integer(col);
        return ri.hashCode() + rc.hashCode();
    }
}
