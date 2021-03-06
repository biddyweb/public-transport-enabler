/*
 * Copyright 2010-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.dto;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Andreas Schildbach
 */
public final class Location implements Serializable
{
	public final LocationType type;
	public final String id;
	public final int lat, lon;
	public final String place;
	public final String name;

	public Location(final LocationType type, final String id, final int lat, final int lon, final String place, final String name)
	{
		this.type = type;
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.place = place;
		this.name = name;

		if (name == null && place != null)
			throw new IllegalArgumentException("places without name cannot exist");
	}

	public Location(final LocationType type, final String id, final String place, final String name)
	{
		this.type = type;
		this.id = id;
		this.lat = 0;
		this.lon = 0;
		this.place = place;
		this.name = name;

		if (name == null && place != null)
			throw new IllegalArgumentException("places without name cannot exist");
	}

	public Location(final LocationType type, final String id, final int lat, final int lon)
	{
		this.type = type;
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.place = null;
		this.name = null;
	}

	public Location(final LocationType type, final String id)
	{
		this.type = type;
		this.id = id;
		this.lat = 0;
		this.lon = 0;
		this.place = null;
		this.name = null;
	}

	public Location(final LocationType type, final int lat, final int lon)
	{
		this.type = type;
		this.id = null;
		this.lat = lat;
		this.lon = lon;
		this.place = null;
		this.name = null;
	}

	public final boolean hasId()
	{
		return id != null;
	}

	public final boolean hasLocation()
	{
		return lat != 0 || lon != 0;
	}

	public final boolean hasName()
	{
		return name != null;
	}

	public final boolean isIdentified()
	{
		if (type == LocationType.STATION)
			return hasId();

		if (type == LocationType.POI)
			return true;

		if (type == LocationType.ADDRESS)
			return hasLocation();

		return false;
	}

	private static final String[] NON_UNIQUE_NAMES = { "Hauptbahnhof", "Hbf", "Bahnhof", "Bf", "Busbahnhof", "ZOB", "Zentrum", "Dorf", "Kirche",
			"Nord", "Ost", "Süd", "West" };
	static
	{
		Arrays.sort(NON_UNIQUE_NAMES);
	}

	public final String uniqueShortName()
	{
		if (place != null && name != null && Arrays.binarySearch(NON_UNIQUE_NAMES, name) >= 0)
			return place + ", " + name;
		else if (name != null)
			return name;
		else if (hasId())
			return id;
		else
			return null;
	}

	@Override
	public String toString()
	{
		return "[" + type + " " + id + " " + lat + "/" + lon + " " + (place != null ? "\"" + place + "\"" : "null") + " "
				+ (name != null ? "\"" + name + "\"" : "null") + "]";
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;
		if (!(o instanceof Location))
			return false;
		final Location other = (Location) o;
		if (this.type != other.type)
			return false;
		if (this.id != null)
			return this.id.equals(other.id);
		if (this.lat != 0 && this.lon != 0)
			return this.lat == other.lat && this.lon == other.lon;

		// only discriminate by name/place if no ids are given
		if (!nullSafeEquals(this.name, other.name))
			return false;
		if (!nullSafeEquals(this.place, other.place))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		int hashCode = 0;
		hashCode += type.hashCode();
		hashCode *= 29;
		if (id != null)
		{
			hashCode += id.hashCode();
		}
		else if (lat != 0 || lon != 0)
		{
			hashCode += lat;
			hashCode *= 29;
			hashCode += lon;
		}
		return hashCode;
	}

	private boolean nullSafeEquals(final Object o1, final Object o2)
	{
		if (o1 == null && o2 == null)
			return true;
		if (o1 != null && o1.equals(o2))
			return true;
		return false;
	}

	private int nullSafeHashCode(final Object o)
	{
		if (o == null)
			return 0;
		return o.hashCode();
	}
}
