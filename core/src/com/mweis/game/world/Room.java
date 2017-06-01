package com.mweis.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/*
 * https://github.com/fisherevans/ProceduralGeneration/blob/master/dungeons/src/main/java/com/fisherevans/procedural_generation/dungeons/Room.java
 * This is a variant of the /fisherevans/'s Room class, which was used alongside the DungeonFactory for random generation.
 * This class has more build on top of it.
 */
public class Room implements Comparable<Room> {
	private int _left, _right, _top, _bottom;
	private Rectangle bounds = new Rectangle();
	private float centerX, centerY;
	private Vector2 center = new Vector2();
	
	public enum RoomType { CRITICAL, NONCRITICAL, HALLWAY; }
	private RoomType type;
	
//	private Doorway leftDoorway, rightDoorway, topDoorway, bottomDoorway;
	
	public Room(int x1, int y1, int width, int height) {		
        this(x1, y1, width, height, null);
	}
	
	public Room(int x1, int y1, int width, int height, RoomType type) {		
        _left = x1;
        _right = x1;
        if(width < 0)
            _left +=  width;
        else
            _right +=  width;

		_bottom = y1;
		_top = y1;
        if(height < 0)
            _bottom +=  height;
        else
            _top +=  height;
        updateBounds();
        this.type = type;
	}
	
	public boolean touches(Room b) {
		return touches(b, 0);
	}
	
	public boolean touches(Room b, int padding) {
		return !(b.getLeft()-padding >= this.getRight() ||
				b.getRight() <= this.getLeft()-padding ||
				b.getTop() <= this.getBottom()-padding ||
				b.getBottom()-padding >= this.getTop());
	}
	
	public boolean overlaps(Rectangle area) {
		return bounds.overlaps(area);
//		return !(area.getX() >= this.getRight() ||
//				area.getX() + area.getWidth() <= this.getLeft() ||
//				area.getY() + area.getHeight() <= this.getBottom() ||
//				area.getY() >= this.getTop());				
	}
	
	/*
	 * True if they intersect, but the area is not contained within th
	 */
	public boolean borderTouches(Rectangle area) {
		return area.overlaps(bounds) && !area.contains(bounds) && !bounds.contains(area);
	}
	
	public void expand(int by) {
		_left -= by;
		_right += by;
		_top += by;
		_bottom -= by;
		updateBounds();
	}

    public int getArea() {
        return getWidth()*getHeight();
    }
	
	public int getWidth() {
		return _right - _left;
	}
	
	public int getHeight() {
		return _top - _bottom;
	}
	
	public int getLeft() {
		return _left;
	}
	
	public int getRight() {
		return _right;
	}
	
	public int getTop() {
		return _top;
	}
	
	public int getBottom() {
		return _bottom;
	}
	
	public float getCenterX() {
		return centerX;
	}
	
	public float getCenterY() {
		return centerY;
	}
	
	public Vector2 getCenter() {
		return center;
	}
	
	public Rectangle getBounds() {
		return bounds;
	}
	
	public RoomType getType() {
		return type;
	}
	
	public void setType(RoomType type) {
		this.type = type;
	}
	
//	public boolean hasLeftDoorway() {
//		return leftDoorway != null;
//	}
//	
//	public Doorway getLeftDoorway() {
//		return leftDoorway;
//	}
//	
//	public void setLeftDoorway(Doorway leftDoorway) {
//		this.leftDoorway = leftDoorway;
//	}
//
//	public boolean hasRightDoorway() {
//		return rightDoorway != null;
//	}
//	
//	public Doorway getRightDoorway() {
//		return rightDoorway;
//	}
//	
//	public void setRightDoorway(Doorway rightDoorway) {
//		this.rightDoorway = rightDoorway;
//	}
//	
//	public boolean hasTopDoorway() {
//		return topDoorway != null;
//	}
//	
//	public Doorway getTopDoorway() {
//		return topDoorway;
//	}
//	
//	public void setTopDoorway(Doorway topDoorway) {
//		this.topDoorway = topDoorway;
//	}
//	
//	public boolean hasBottomDoorway() {
//		return bottomDoorway != null;
//	}
//	
//	public Doorway getBottomDoorway() {
//		return bottomDoorway;
//	}
//	
//	public void setBottomDoorway(Doorway bottomDoorway) {
//		this.bottomDoorway = bottomDoorway;
//	}
	
	public void shift(int x, int y) {
		_left += x;
		_right += x;
		
		_top += y;
		_bottom += y;
		updateBounds();
	}

    public double getRatio() {
        return getRatio(getWidth(), getHeight());
    }

    public static double getRatio(int width, int height) {
        if(width > height)
            return width/((double)height);
        else
            return height/((double)width);
    }
	
	public String toString() {
		return String.format("[L:%d,  R:%d, T:%d, B:%d]", _left, _right, _top, _bottom);
	}

    public int compareTo(Room room) {
        int d = room.getArea() - getArea();
        if(d == 0) {
            return (int) Math.signum((room.getCenterX()*room.getCenterX()+room.getCenterY()*room.getCenterY())
                    -(getCenterX()*getCenterX()+getCenterY()*getCenterY()));
        } else
            return d;
    }
    
    private void updateBounds() {
    	bounds.set(_left, _bottom, _right - _left, _top - _bottom);
    	centerX = (_left+_right)/2.0f;
    	centerY = (_top+_bottom)/2.0f;
    	center.set(centerX, centerY);
    }
}
