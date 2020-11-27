package Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.ShapefileLoader;

public class GISFunctions
{
	/**
	 * Nice generic function :-) that reads in objects from shapefiles.
	 * <p>
	 * The objects (agents) created must extend FixedGeography to guarantee that
	 * they will have a setCoords() method. This is necessary because, for
	 * simplicity, geographical objects which don't move store their coordinates
	 * alongside the projection which stores them as well. So the coordinates
	 * must be set manually by this function once the shapefile has been read
	 * and the objects have been given coordinates in their projection.
	 * 
	 * @param <T>
	 *            The type of object to be read (e.g. PecsHouse). Must exted
	 * @param cl
	 *            The class of the building being read (e.g. PecsHouse.class).
	 * @param shapefileLocation
	 *            The location of the shapefile containing the objects.
	 * @param geog
	 *            A geography to add the objects to.
	 * @param context
	 *            A context to add the objects to.
	 * @throws MalformedURLException
	 *             If the location of the shapefile cannot be converted into a
	 *             URL
	 * @throws FileNotFoundException
	 *             if the shapefile does not exist.
	 * @see FixedGeography
	 */
	public static <T extends FixedGeography> void readShapefile(Class<T> cl, String shapefileLocation, Geography<T> geog, Context<T> context)
					throws MalformedURLException, FileNotFoundException
	{
		File shapefile = null;
		ShapefileLoader<T> loader = null;
		shapefile = new File(shapefileLocation);
		if (!shapefile.exists( ))
		{
			throw new FileNotFoundException("Could not find the given shapefile: " + shapefile.getAbsolutePath( ));
		}
		loader = new ShapefileLoader<T>(cl, shapefile.toURI( ).toURL( ), geog, context);
		while (loader.hasNext( ))
		{
			loader.next( );
		}
		for (T obj : context.getObjects(cl))
		{
			obj.setCoords(geog.getGeometry(obj).getCentroid( ).getCoordinate( ));
		}
	}
}
