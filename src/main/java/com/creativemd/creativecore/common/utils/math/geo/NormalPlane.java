package com.creativemd.creativecore.common.utils.math.geo;

import javax.vecmath.Vector3f;

import com.creativemd.creativecore.common.utils.math.RotationUtils;
import com.creativemd.creativecore.common.utils.math.vec.VectorFan;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;

public class NormalPlane {
	
	public final Vector3f normal;
	public final Vector3f origin;
	
	public NormalPlane(Vector3f origin, Vector3f normal) {
		this.origin = origin;
		this.normal = new Vector3f(normal);
		this.normal.normalize();
	}
	
	public NormalPlane(Axis axis, float value, EnumFacing facing) {
		this.origin = new Vector3f();
		RotationUtils.setValue(origin, value, axis);
		this.normal = new Vector3f();
		RotationUtils.setValue(normal, facing.getAxisDirection().getOffset(), facing.getAxis());
	}
	
	public Boolean isInFront(Vector3f vec) {
		Vector3f temp = new Vector3f(vec);
		temp.sub(origin);
		float result = normal.dot(temp);
		if (result < 0 ? (result > -VectorFan.EPSILON) : (result < VectorFan.EPSILON))
			return null;
		return result > 0;
	}
	
	public boolean cuts(VectorFan strip) {
		boolean front = false;
		boolean back = false;
		for (int i = 0; i < strip.count(); i++) {
			Boolean result = isInFront(strip.get(i));
			
			if (result == null)
				return true;
			
			if (result)
				front = true;
			if (!result)
				back = true;
			
			if (front && back)
				return true;
		}
		return false;
	}
	
	public Vector3f intersect(Vector3f start, Vector3f end) {
		Vector3f lineOrigin = start;
		Vector3f lineDirection = new Vector3f(end);
		lineDirection.sub(lineOrigin);
		lineDirection.normalize();
		
		if (normal.dot(lineDirection) == 0)
			return null;
		
		float t = (normal.dot(origin) - normal.dot(lineOrigin)) / normal.dot(lineDirection);
		Vector3f point = new Vector3f(lineDirection);
		point.scale(t);
		point.add(lineOrigin);
		return point;
	}
	
	public Vector3f intersect(Ray3d ray) {
		if (normal.dot(ray.direction) == 0)
			return null;
		
		float t = (normal.dot(origin) - normal.dot(ray.origin)) / normal.dot(ray.direction);
		Vector3f point = new Vector3f(ray.direction);
		point.scale(t);
		point.add(ray.origin);
		return point;
	}
	
	@Override
	public String toString() {
		return "[o:" + origin + ",n:" + normal + "]";
	}
	
}
