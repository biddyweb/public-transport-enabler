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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Departure;
import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.LocationType;
import de.schildbach.pte.dto.NearbyStationsResult;
import de.schildbach.pte.dto.Position;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryDeparturesResult;
import de.schildbach.pte.dto.QueryDeparturesResult.Status;
import de.schildbach.pte.dto.ResultHeader;
import de.schildbach.pte.dto.StationDepartures;
import de.schildbach.pte.util.ParserUtils;

/**
 * @author Andreas Schildbach
 */
public class ShProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SH;
	private static final String API_BASE = "http://nah.sh.hafas.de/bin/";

	private static final long PARSER_DAY_ROLLOVER_THRESHOLD_MS = 12 * 60 * 60 * 1000;

	public ShProvider()
	{
		super(API_BASE + "stboard.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 10, UTF_8);

		setStationBoardCanDoEquivs(false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1)
			return 'I';
		if (value == 2)
			return 'I';
		if (value == 4)
			return 'I';
		if (value == 8)
			return 'R';
		if (value == 16)
			return 'S';
		if (value == 32)
			return 'B';
		if (value == 64)
			return 'F';
		if (value == 128)
			return 'U';
		if (value == 256)
			return 'T';
		if (value == 512)
			return 'P';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Hochgeschwindigkeitszug
			productBits.setCharAt(1, '1'); // IC/EC
			productBits.setCharAt(2, '1'); // Fernverkehrszug
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(3, '1'); // Regionalverkehrszug
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(4, '1'); // S-Bahn
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(7, '1'); // U-Bahn
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(8, '1'); // Stadtbahn
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(5, '1'); // Bus
		}
		else if (product == Product.ON_DEMAND)
		{
			productBits.setCharAt(9, '1'); // Anruf-Sammel-Taxi
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(6, '1'); // Schiff
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final String[] PLACES = { "Hamburg", "Kiel", "Lübeck", "Flensburg", "Neumünster" };

	@Override
	protected String[] splitStationName(final String name)
	{
		for (final String place : PLACES)
		{
			if (name.startsWith(place + " ") || name.startsWith(place + "-"))
				return new String[] { place, name.substring(place.length() + 1) };
		}

		return super.splitStationName(name);
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher mComma = P_SPLIT_NAME_FIRST_COMMA.matcher(address);
		if (mComma.matches())
			return new String[] { mComma.group(1), mComma.group(2) };

		return super.splitStationName(address);
	}

	@Override
	public NearbyStationsResult queryNearbyStations(final Location location, final int maxDistance, final int maxStations) throws IOException
	{
		if (location.type == LocationType.STATION && location.hasId())
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

	private static final Pattern P_DEPARTURES_HEAD_COARSE = Pattern.compile(".*?" //
			+ "(?:" //
			+ "Bhf\\./Haltest\\.:</span>\\n<span class=\"output\">([^<]*)<.*?" // location
			+ "Fahrplan:</span>\\n<span class=\"output\">\n" //
			+ "(\\d{2}\\.\\d{2}\\.\\d{2})[^\n]*,\n" // date
			+ "Abfahrt (\\d{1,2}:\\d{2}).*?" // time
			+ "<table class=\"resultTable\"[^>]*>(.+?)</table>" // content
			+ "|(verkehren an dieser Haltestelle keine)" //
			+ "|(Eingabe kann nicht interpretiert)" //
			+ "|(Verbindung zum Server konnte leider nicht hergestellt werden|kann vom Server derzeit leider nicht bearbeitet werden))" //
			+ ".*?" //
	, Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_COARSE = Pattern.compile("<tr class=\"(depboard-\\w*)\">(.*?)</tr>", Pattern.DOTALL);
	private static final Pattern P_DEPARTURES_FINE = Pattern.compile("\n" //
			+ "<td class=\"time\">(\\d{1,2}:\\d{2}).*?" // plannedTime
			+ "<img class=\"product\" src=\"/hafas-res/[^\"]*?(\\w+)_pic\\.png\"[^>]*>\\s*([^<]*)<.*?" // type,line
			+ "<a href=\"http://nah\\.sh\\.hafas\\.de/bin/stboard\\.exe/dn\\?input=(\\d+)&[^>]*>\n" // destinationId
			+ "([^\n]*)\n.*?" // destination
			+ "(?:<td class=\"center sepline top\">\n(" + ParserUtils.P_PLATFORM + ").*?)?" // position
	, Pattern.DOTALL);

	@Override
	public QueryDeparturesResult queryDepartures(final String stationId, final Date time, final int maxDepartures, final boolean equivs)
			throws IOException
	{
		final ResultHeader header = new ResultHeader(SERVER_PRODUCT);
		final QueryDeparturesResult result = new QueryDeparturesResult(header);

		// scrape page
		final StringBuilder uri = new StringBuilder(stationBoardEndpoint);
		appendXmlStationBoardParameters(uri, time, stationId, maxDepartures, false, null);
		final CharSequence page = ParserUtils.scrape(uri.toString());

		// System.out.println(uri);
		// System.out.println(page);

		// parse page
		final Matcher mHeadCoarse = P_DEPARTURES_HEAD_COARSE.matcher(page);
		if (mHeadCoarse.matches())
		{
			// messages
			if (mHeadCoarse.group(5) != null)
			{
				result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId),
						Collections.<Departure> emptyList(), null));
				return result;
			}
			else if (mHeadCoarse.group(6) != null)
				return new QueryDeparturesResult(header, Status.INVALID_STATION);
			else if (mHeadCoarse.group(7) != null)
				return new QueryDeparturesResult(header, Status.SERVICE_DOWN);

			final String[] placeAndName = splitStationName(ParserUtils.resolveEntities(mHeadCoarse.group(1)));
			final Calendar currentTime = new GregorianCalendar(timeZone);
			currentTime.clear();
			ParserUtils.parseGermanDate(currentTime, mHeadCoarse.group(2));
			ParserUtils.parseEuropeanTime(currentTime, mHeadCoarse.group(3));

			final List<Departure> departures = new ArrayList<Departure>(8);
			String oldZebra = null;

			final Matcher mDepCoarse = P_DEPARTURES_COARSE.matcher(mHeadCoarse.group(4));
			while (mDepCoarse.find())
			{
				final String zebra = mDepCoarse.group(1);
				if (oldZebra != null && zebra.equals(oldZebra))
					throw new IllegalArgumentException("missed row? last:" + zebra);
				else
					oldZebra = zebra;

				final Matcher mDepFine = P_DEPARTURES_FINE.matcher(mDepCoarse.group(2));
				if (mDepFine.matches())
				{
					final Calendar plannedTime = new GregorianCalendar(timeZone);
					plannedTime.setTimeInMillis(currentTime.getTimeInMillis());
					ParserUtils.parseEuropeanTime(plannedTime, mDepFine.group(1));

					if (plannedTime.getTimeInMillis() - currentTime.getTimeInMillis() < -PARSER_DAY_ROLLOVER_THRESHOLD_MS)
						plannedTime.add(Calendar.DAY_OF_MONTH, 1);

					final String lineType = mDepFine.group(2);
					final char product = intToProduct(Integer.parseInt(lineType));
					final String lineStr = Character.toString(product) + normalizeLineName(mDepFine.group(3).trim());
					final Line line = new Line(null, lineStr, lineStyle(null, lineStr));

					final String destinationId = mDepFine.group(4);
					final String destinationName = ParserUtils.resolveEntities(mDepFine.group(5));
					final Location destination;
					if (destinationId != null)
					{
						final String[] destinationPlaceAndName = splitStationName(destinationName);
						destination = new Location(LocationType.STATION, destinationId, destinationPlaceAndName[0], destinationPlaceAndName[1]);
					}
					else
					{
						destination = new Location(LocationType.ANY, null, null, destinationName);
					}

					final Position position = mDepFine.group(6) != null ? new Position("Gl. " + ParserUtils.resolveEntities(mDepFine.group(6)))
							: null;

					final Departure dep = new Departure(plannedTime.getTime(), null, line, position, destination, null, null);

					if (!departures.contains(dep))
						departures.add(dep);
				}
				else
				{
					throw new IllegalArgumentException("cannot parse '" + mDepCoarse.group(2) + "' on " + stationId);
				}
			}

			result.stationDepartures.add(new StationDepartures(new Location(LocationType.STATION, stationId, placeAndName[0], placeAndName[1]),
					departures, null));
			return result;
		}
		else
		{
			throw new IllegalArgumentException("cannot parse '" + page + "' on " + stationId);
		}
	}

	@Override
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("IXB".equals(ucType)) // ICE
			return 'I';
		if ("ECW".equals(ucType)) // EC
			return 'I';
		if ("DPF".equals(ucType)) // Hamburg-Koeln-Express
			return 'I';
		if ("RRT".equals(ucType)) // TGV
			return 'I';

		if ("ZRB".equals(ucType)) // Zahnradbahn
			return 'R';

		if ("KBS".equals(ucType))
			return 'B';
		if ("KB1".equals(ucType))
			return 'B';
		if ("KLB".equals(ucType))
			return 'B';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
