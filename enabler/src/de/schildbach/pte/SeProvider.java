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

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schildbach.pte.dto.Line;
import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class SeProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SE;
	private static final String API_BASE = "http://samtrafiken.hafas.de/bin/";

	// http://reseplanerare.resrobot.se/bin/
	// http://api.vasttrafik.se/bin/

	public SeProvider()
	{
		super(API_BASE + "stboard.exe/sn", API_BASE + "ajax-getstop.exe/sny", API_BASE + "query.exe/sn", 14, UTF_8);

		setClientType("ANDROID");
		setUseIso8601(true);
		setStationBoardHasStationTable(false);
		setStationBoardCanDoEquivs(false);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected char intToProduct(final int value)
	{
		if (value == 1) // Flyg
			return 'I';
		if (value == 2) // X2000
			return 'I';
		if (value == 4)
			return 'R';
		if (value == 8) // Expressbus
			return 'B';
		if (value == 16)
			return 'R';
		if (value == 32) // Tunnelbana
			return 'U';
		if (value == 64) // Spårvagn
			return 'T';
		if (value == 128)
			return 'B';
		if (value == 256)
			return 'F';
		if (value == 512) // Länstaxi
			return 'F';
		if (value == 1024) // Future
			return 'R';

		throw new IllegalArgumentException("cannot handle: " + value);
	}

	@Override
	protected void setProductBits(final StringBuilder productBits, final Product product)
	{
		if (product == Product.HIGH_SPEED_TRAIN)
		{
			productBits.setCharAt(0, '1'); // Flyg
			productBits.setCharAt(1, '1'); // Snabbtåg
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1'); // Tåg
			productBits.setCharAt(4, '1'); // Lokaltåg
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(5, '1'); // Tunnelbana
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(6, '1'); // Spårvagn
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(3, '1'); // Expressbuss
			productBits.setCharAt(7, '1'); // Buss
		}
		else if (product == Product.ON_DEMAND)
		{
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(8, '1'); // Båt
		}
		else if (product == Product.CABLECAR)
		{
		}
		else
		{
			throw new IllegalArgumentException("cannot handle: " + product);
		}
	}

	private static final Pattern P_SPLIT_NAME_PAREN = Pattern.compile("(.*) \\((.{3,}?) kn\\)");

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher mParen = P_SPLIT_NAME_PAREN.matcher(name);
		if (mParen.matches())
			return new String[] { mParen.group(2), mParen.group(1) };

		return super.splitStationName(name);
	}

	@Override
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}

	@Override
	protected String[] splitAddress(final String address)
	{
		final Matcher mComma = P_SPLIT_NAME_LAST_COMMA.matcher(address);
		if (mComma.matches())
			return new String[] { mComma.group(2), mComma.group(1) };

		return super.splitStationName(address);
	}

	private static final Pattern P_NORMALIZE_LINE_BUS = Pattern.compile("Buss\\s*(.*)");
	private static final Pattern P_NORMALIZE_LINE_SUBWAY = Pattern.compile("Tunnelbana\\s*(.*)");

	@Override
	protected Line parseLineAndType(final String line)
	{
		final Matcher mBus = P_NORMALIZE_LINE_BUS.matcher(line);
		if (mBus.matches())
			return newLine('B', mBus.group(1), null);

		final Matcher mSubway = P_NORMALIZE_LINE_SUBWAY.matcher(line);
		if (mSubway.matches())
			return newLine('U', "T" + mSubway.group(1), null);

		return newLine('?', line, null);
	}
}
