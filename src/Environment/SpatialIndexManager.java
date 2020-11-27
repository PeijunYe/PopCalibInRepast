/*
�Copyright 2012 Nick Malleson
This file is part of RepastCity.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
*/
package Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

import repast.simphony.space.gis.Geography;

/**
 * Class that can be used to hold spatial indexes for Geography Projections.
 * This allows for more efficient GIS operations (e.g. finding nearby objects).
 * The inner <code>Index</code> class is used to actually hold the index.
 * 
 * @author Nick Malleson
 * @see SpatialIndex
 * @see STRtree
 */
public abstract class SpatialIndexManager implements Cacheable
{
	// Link spatial indices to their geographies.
	private static Map<Geography<?>, Index<?>> indices = new HashMap<Geography<?>, Index<?>>( );

	/**
	 * Create a new spatial index for the given geography
	 * <code>Geography</code>.
	 * 
	 * @param geog
	 * @param clazz
	 *            The class of the objects that are being stored in the
	 *            geography.
	 * @param <T>
	 *            The type of object stored in the geography.
	 */
	public static <T> void createIndex(Geography<T> geog, Class<T> clazz)
	{
		Index<T> i = new Index<T>(geog, clazz);
		SpatialIndexManager.indices.put(geog, i);
	}

	/**
	 * Search for objects located at the given coordinate. This uses the
	 * <code>query()</code> function of the underlying spatial index so might
	 * return objects that are close two, but do not interesect, the coordinate.
	 * 
	 * @param <T>
	 *            The type of object that will be returned.
	 * @param x
	 *            The coordinate to search around
	 * @param geography
	 *            The given geography to look through
	 * @return The objects that intersect the coordinate (or are close to it) or
	 *         an empty list if none could be found.
	 * @throws NoSuchElementException
	 *             If there is no spatial index for the given geography.
	 * @see STRtree
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> List<T> search(Geography<T> geog, Geometry geom) throws NoSuchElementException
	{
		Index<T> index = (Index<T>) indices.get(geog);
		if (index == null)
		{
			throw new NoSuchElementException("The geometry " + geog.getName( ) + " does not have a spatial index.");
		}
		// Query the spatial index for the nearest objects.
		List<Geometry> close = index.si.query(geom.getEnvelopeInternal( ));
		List<T> objects = new ArrayList<T>( );
		for (Geometry g : close)
		{
			objects.add(index.lookupFeature(g));
		}
		return objects;
	}

	/**
	 * Find out whether or not this <code>SpatialIndexManager</code> has an
	 * index for the given geography.
	 */
	public static boolean hasIndex(Geography<?> geog)
	{
		return indices.containsKey(geog);
	}

	@Override
	public void clearCaches( )
	{
		indices.clear( );
	}
}

/**
 * Inner class used for convenience to store each spatial index as well as other
 * useful information (e.g. maintain a link between the geometries in the index
 * and the actual objects that these geometries belong to.
 * 
 * @author Nick Malleson
 *
 * @param <T>
 *            The type of object being stored (e.g. House, Road, etc).
 */
class Index<T>
{
	/*
	 * The actual spatial index.
	 */
	SpatialIndex si;
	/*
	 * A lookup relating Geometrys to the objects that they represent.
	 */
	private Map<Geometry, T> featureLookup;

	public Index(Geography<T> geog, Class<T> clazz)
	{
		this.si = new STRtree( );
		this.featureLookup = new HashMap<Geometry, T>( );
		this.createIndex(geog, clazz);
	}

	// Run through each object in the geography and add them to the spatial
	// index.
	private void createIndex(Geography<T> geog, Class<T> clazz)
	{
		Geometry geom;
		Envelope bounds;
		for (T t : geog.getAllObjects( ))
		{
			geom = (Geometry) geog.getGeometry(t);
			bounds = geom.getEnvelopeInternal( );
			this.si.insert(bounds, geom);
			this.featureLookup.put(geom, t);
		}
	}

	public T lookupFeature(Geometry geom) throws NoSuchElementException
	{
		assert this.featureLookup.containsKey(geom) : "Internal error: for some reason the " + "given geometry is not a key in the feature lookup table.";
		return this.featureLookup.get(geom);
	}
}
