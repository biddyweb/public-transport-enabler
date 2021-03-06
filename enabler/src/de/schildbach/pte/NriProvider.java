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

package de.schildbach.pte;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;

/**
 * @author Andreas Schildbach
 */
public class NriProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.NRI;
	private static final String API_BASE = "http://hafas.websrv05.reiseinfo.no/bin/dev/nri/";

	public NriProvider()
	{
		super(API_BASE + "stboard.exe/on", API_BASE + "ajax-getstop.exe/ony", API_BASE + "query.exe/on", 8);

		setJsonGetStopsEncoding(UTF_8);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1) // Air
			return 'I';
		if (value == 2)
			return 'R';
		if (value == 4)
			return 'B';
		if (value == 8)
			return 'T';
		if (value == 16)
			return 'U';
		if (value == 32)
			return 'F';
		if (value == 64)
			return 'F';
		if (value == 128)
			return 'F';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Flugzeug
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(1, '1'); // Regionalverkehrszug
			productBits.setCharAt(7, '1'); // Tourismus-Züge
			productBits.setCharAt(2, '1'); // undokumentiert
		}
		else if (product == Product.SUBURBAN_TRAIN || product == Product.TRAM)
		{
			productBits.setCharAt(3, '1'); // Stadtbahn
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(4, '1'); // U-Bahn
		}
		else if (product == Product.BUS || product == Product.ON_DEMAND)
		{
			productBits.setCharAt(2, '1'); // Bus
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(5, '1'); // Express-Boot
			productBits.setCharAt(6, '1'); // Schiff
			productBits.setCharAt(7, '1'); // Fähre
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Oslo", "Bergen" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
			if (name.startsWith(place + " "))
				return new String[] { place, name.substring(place.length() + 1) };

		return super.splitStationName(name);
	}

	@Override
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	public QueryTripsResult queryTrips(final Location from, final Location via, final Location to, final Date date, final boolean dep,
			final Collection<Product> products, final WalkSpeed walkSpeed, final Accessibility accessibility, final Set<Option> options)
			throws IOException
	{
		return queryTripsXml(from, via, to, date, dep, products, walkSpeed, accessibility, options);
	}

	@Override
	public QueryTripsResult queryMoreTrips(final QueryTripsContext context, final boolean later) throws IOException
	{
		return queryMoreTripsXml(context, later);
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("AIR".equals(ucType))
			return 'I';

		if ("TRA".equals(ucType))
			return 'R';
		if ("TRAIN".equals(ucType))
			return 'R';
		if ("HEL".equals(ucType)) // Heli
			return 'R';

		if ("U".equals(ucType))
			return 'U';

		if ("TRAM".equals(ucType))
			return 'T';
		if ("MTR".equals(ucType))
			return 'T';

		if (ucType.startsWith("BUS"))
			return 'B';

		if ("EXP".equals(ucType))
			return 'F';
		if ("EXP.BOAT".equals(ucType))
			return 'F';
		if ("FERRY".equals(ucType))
			return 'F';
		if ("FER".equals(ucType))
			return 'F';
		if ("SHIP".equals(ucType))
			return 'F';
		if ("SHI".equals(ucType))
			return 'F';

		return 0;
	}
}
