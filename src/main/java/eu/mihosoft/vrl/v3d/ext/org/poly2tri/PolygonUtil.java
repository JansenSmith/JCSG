/**
 * PolygonUtil.java
 *
 * Copyright 2014-2014 Michael Hoffer info@michaelhoffer.de. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer info@michaelhoffer.de "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer info@michaelhoffer.de OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Michael Hoffer
 * info@michaelhoffer.de.
 */
package eu.mihosoft.vrl.v3d.ext.org.poly2tri;

import eu.mihosoft.vrl.v3d.Extrude;
import eu.mihosoft.vrl.v3d.Plane;
import eu.mihosoft.vrl.v3d.Polygon;
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.Vertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

// TODO: Auto-generated Javadoc
/**
 * The Class PolygonUtil.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class PolygonUtil {

	/**
	 * Instantiates a new polygon util.
	 */
	private PolygonUtil() {
		throw new AssertionError("Don't instantiate me!", null);
	}

	/**
	 * Converts a CSG polygon to a poly2tri polygon (including holes).
	 *
	 * @param polygon the polygon to convert
	 * @return a CSG polygon to a poly2tri polygon (including holes)
	 */
	public static eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon fromCSGPolygon(eu.mihosoft.vrl.v3d.Polygon polygon) {

		// convert polygon
		List<PolygonPoint> points = new ArrayList<>();
		for (Vertex v : polygon.vertices) {
			PolygonPoint vp = new PolygonPoint(v.pos.x, v.pos.y, v.pos.z);
			points.add(vp);
		}

		eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon result = new eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon(points);

		// convert holes
		Optional<List<eu.mihosoft.vrl.v3d.Polygon>> holesOfPresult = polygon.getStorage()
				.getValue(eu.mihosoft.vrl.v3d.Edge.KEY_POLYGON_HOLES);
		if (holesOfPresult.isPresent()) {
			List<eu.mihosoft.vrl.v3d.Polygon> holesOfP = holesOfPresult.get();

			holesOfP.stream().forEach((hP) -> {
				result.addHole(fromCSGPolygon(hP));
			});
		}

		return result;
	}

	/**
	 * Concave to convex.
	 *
	 * @param concave the concave
	 * @return the list
	 */
	public static List<eu.mihosoft.vrl.v3d.Polygon> concaveToConvex(eu.mihosoft.vrl.v3d.Polygon incoming) {
		incoming=pruneDuplicatePoints(incoming);
		if(incoming==null)
			return new ArrayList<>();
		if (incoming.vertices.size() < 3)
			return new ArrayList<>();
		eu.mihosoft.vrl.v3d.Polygon concave;
		boolean xnorm = Math.abs(incoming.plane.normal.x) >= 1.0 - Plane.EPSILON;
		Transform orentationInv = null;
		if (xnorm) {
			Transform orentation = new Transform().roty(incoming.plane.normal.x * 90);// th triangulation function needs
																						// the polygon on the xy plane
			orentationInv = orentation.inverse();
			concave = incoming.transformed(orentation);
		} else
			concave = incoming;

		List<eu.mihosoft.vrl.v3d.Polygon> result = new ArrayList<>();

		Vector3d normal = concave.vertices.get(0).normal.clone();

		boolean cw = !Extrude.isCCW(concave);
		concave = Extrude.toCCW(concave);

		eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon p = fromCSGPolygon(concave);

		eu.mihosoft.vrl.v3d.ext.org.poly2tri.Poly2Tri.triangulate(p);

		List<DelaunayTriangle> triangles = p.getTriangles();

		List<Vertex> triPoints = new ArrayList<>();

		for (DelaunayTriangle t : triangles) {

			int counter = 0;
			for (TriangulationPoint tp : t.points) {

				triPoints.add(new Vertex(new Vector3d(tp.getX(), tp.getY(), tp.getZ()), normal));

				if (counter == 2) {
					if (!cw) {
						Collections.reverse(triPoints);
					}
					eu.mihosoft.vrl.v3d.Polygon poly = new eu.mihosoft.vrl.v3d.Polygon(triPoints, concave.getStorage());
					if (xnorm)
						result.add(poly.transform(orentationInv));
					else
						result.add(poly);
					counter = 0;
					triPoints = new ArrayList<>();

				} else {
					counter++;
				}
			}
		}

		return result;
	}

	private static Polygon pruneDuplicatePoints(Polygon incoming) {
		ArrayList<Vertex> newPoints = new ArrayList<Vertex>();
		for(int i=0;i<incoming.vertices.size();i++) {
			Vertex v=incoming.vertices.get(i);
			boolean duplicate=false;
			for(Vertex vx:newPoints) {
				if(vx.pos.test(v.pos)) {
					duplicate=true;
				}
			}
			if(!duplicate) {
				newPoints.add(v);
			}
		
		}
		try{
			return new Polygon(newPoints);
		}catch(java.lang.IndexOutOfBoundsException ex) {
			return null;
		}
	}
}
