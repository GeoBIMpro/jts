package com.vividsolutions.jtstest.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.noding.BasicSegmentString;
import com.vividsolutions.jts.noding.FastNodingValidator;
import com.vividsolutions.jts.noding.InteriorIntersectionFinder;
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.Noder;
import com.vividsolutions.jts.noding.ScaledNoder;
import com.vividsolutions.jts.noding.SegmentStringUtil;
import com.vividsolutions.jts.noding.snapround.GeometryNoder;
import com.vividsolutions.jts.noding.snapround.MCIndexSnapRounder;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

public class NodingFunctions 
{

  /**
   * Reduces precision pointwise, then snap-rounds.
   * Note that output set may not contain non-unique linework
   * (and thus cannot be used as input to Polygonizer directly).
   * UnaryUnion is one way to make the linework unique.
   * 
   * 
   * @param geom a geometry containing linework to node
   * @param scaleFactor the precision model scale factor to use
   * @return the noded, snap-rounded linework
   */
  public static Geometry snapRoundWithPointwisePrecisionReduction(
      Geometry geom, double scaleFactor) {
    PrecisionModel pm = new PrecisionModel(scaleFactor);

    Geometry roundedGeom = GeometryPrecisionReducer.reducePointwise(geom, pm);

    List geomList = new ArrayList();
    geomList.add(roundedGeom);

    GeometryNoder noder = new GeometryNoder(pm);
    List lines = noder.node(geomList);

    return FunctionsUtil.getFactoryOrDefault(geom).buildGeometry(lines);
  }
	
  public static boolean isNodingValid(Geometry geom) {
    FastNodingValidator nv = new FastNodingValidator(
        SegmentStringUtil.extractNodedSegmentStrings(geom));
    return nv.isValid();
  }

  public static Geometry findSingleNodePoint(Geometry geom) {
    FastNodingValidator nv = new FastNodingValidator(
        SegmentStringUtil.extractNodedSegmentStrings(geom));
    nv.isValid();
    List intPts = nv.getIntersections();
    if (intPts.size() == 0) return null;
    return FunctionsUtil.getFactoryOrDefault(geom).createPoint((Coordinate) intPts.get(0));
  }
  
  public static Geometry findNodePoints(Geometry geom)
  {
    List intPts = FastNodingValidator.computeIntersections( 
        SegmentStringUtil.extractNodedSegmentStrings(geom) );    
    return FunctionsUtil.getFactoryOrDefault(null)
        .createMultiPoint(CoordinateArrays.toCoordinateArray(intPts));
  }
	  
  public static int interiorIntersectionCount(Geometry geom)
  {
    InteriorIntersectionFinder intCounter = InteriorIntersectionFinder
    		.createIntersectionCounter( new RobustLineIntersector() );
    Noder noder = new MCIndexNoder( intCounter );
    noder.computeNodes( SegmentStringUtil.extractNodedSegmentStrings(geom) );
    return intCounter.count();
  }

  public static Geometry MCIndexNodingWithPrecision(Geometry geom, double scaleFactor)
  {
    PrecisionModel fixedPM = new PrecisionModel(scaleFactor);
    
    LineIntersector li = new RobustLineIntersector();
    li.setPrecisionModel(fixedPM);

    Noder noder = new MCIndexNoder(new IntersectionAdder(li));
    noder.computeNodes( SegmentStringUtil.extractNodedSegmentStrings(geom) );
    return SegmentStringUtil.toGeometry( noder.getNodedSubstrings(), FunctionsUtil.getFactoryOrDefault(geom) );
  }

  public static Geometry MCIndexNoding(Geometry geom)
  {
    Noder noder = new MCIndexNoder(new IntersectionAdder(new RobustLineIntersector()));
    noder.computeNodes( SegmentStringUtil.extractNodedSegmentStrings(geom) );
    return SegmentStringUtil.toGeometry(noder.getNodedSubstrings(), FunctionsUtil.getFactoryOrDefault(geom));
  }

  /**
   * Runs a ScaledNoder on input.
   * Input vertices should be rounded to precision model.
   * 
   * @param geom
   * @param scaleFactor
   * @return the noded geometry
   */
  public static Geometry scaledNoding(Geometry geom, double scaleFactor)
  {
    List segs = createSegmentStrings(geom);
    PrecisionModel fixedPM = new PrecisionModel(scaleFactor);
    Noder noder = new ScaledNoder(new MCIndexSnapRounder(new PrecisionModel(1.0)),
        fixedPM.getScale());
    noder.computeNodes(segs);
    Collection nodedSegStrings = noder.getNodedSubstrings();
    return SegmentStringUtil.toGeometry(nodedSegStrings, FunctionsUtil.getFactoryOrDefault(geom));
  }

  private static List createSegmentStrings(Geometry geom)
  {
    List segs = new ArrayList();
    List lines = LinearComponentExtracter.getLines(geom);
    for (Iterator i = lines.iterator(); i.hasNext(); ) {
      LineString line = (LineString) i.next();
      segs.add(new BasicSegmentString(line.getCoordinates(), null));
    }
    return segs;
  }
  

}
