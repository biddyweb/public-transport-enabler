/*
 * Copyright 2013-2015 the original author or authors.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Product;

/**
 * Jesuralem? JET = Jerusalem Eternal Tours?
 * 
 * @author Andreas Schildbach
 */
public class JetProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.JET;
	private static final String API_BASE = "http://planner.jet.org.il/bin/";

	public JetProvider()
	{
		super(API_BASE + "stboard.bin/yn", API_BASE + "ajax-getstop.bin/yn", API_BASE + "query.bin/yn", 4, UTF_8);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 4)
			return 'T';
		if (value == 8)
			return 'B';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
		}
		else if (product == Product.SUBWAY)
		{
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(2, '1'); // Stadtbahn
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(3, '1'); // Bus
		}
		else if (product == Product.ON_DEMAND)
		{
		}
		else if (product == Product.FERRY)
		{
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	@Override
	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.hasLocation())
		{
			final StringBuilder uri = new StringBuilder(queryEndpoint);
			appendJsonNearbyStationsParameters(uri, location, maxDistance, maxStations);

			return jsonNearbyStations(uri.toString());
		}
		else if (location.type == LocationType.STATION && location.hasId())
		{
			final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
			uri.append("?near=Anzeigen");
			uri.append("&distance=").append(maxDistance != 0 ? maxDistance / 1000 : 50);
			uri.append("&input=").append(normalizeStationId(location.id));

			return htmlNearbyStations(uri.toString());
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + location);
		}
	}

	private static final Pattern P_NORMALIZE_BUS = Pattern.compile("([א]?\\d{1,3})#");

	@Override
	protected Line parseLineAndType(final String lineAndType)
	{
		if ("רק1#".equals(lineAndType))
			return newLine('T', "רק1", null);

		if ("א 11#".equals(lineAndType) || "11א#".equals(lineAndType))
			return newLine('B', "א11", null);

		final Matcher mBus = P_NORMALIZE_BUS.matcher(lineAndType);
		if (mBus.matches())
			return newLine('B', mBus.group(1), null);

		throw new IllegalStateException("cannot normalize line#type '" + lineAndType + "'");
	}
}
