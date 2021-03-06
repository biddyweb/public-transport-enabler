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

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public final class BahnProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.DB;
	private static final String API_BASE = "http://reiseauskunft.bahn.de/bin/";

	public BahnProvider()
	{
		super(API_BASE + "bhftafel.exe/dn", API_BASE + "ajax-getstop.exe/dn", API_BASE + "query.exe/dn", 14);

		setClientType("ANDROID");
		setStationBoardHasStationTable(false);
		setJsonGetStopsUseWeight(false);
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
			return 'R';
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
			productBits.setCharAt(0, '1');
			productBits.setCharAt(1, '1');
		}
		else if (product == Product.REGIONAL_TRAIN)
		{
			productBits.setCharAt(2, '1');
			productBits.setCharAt(3, '1');
		}
		else if (product == Product.SUBURBAN_TRAIN)
		{
			productBits.setCharAt(4, '1');
		}
		else if (product == Product.SUBWAY)
		{
			productBits.setCharAt(7, '1');
		}
		else if (product == Product.TRAM)
		{
			productBits.setCharAt(8, '1');
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(5, '1');
		}
		else if (product == Product.ON_DEMAND)
		{
			productBits.setCharAt(9, '1');
		}
		else if (product == Product.FERRY)
		{
			productBits.setCharAt(6, '1');
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
	public Collection<Product> defaultProducts()
	{
		return Product.ALL;
	}

	private static final Pattern P_SPLIT_NAME_ONE_COMMA = Pattern.compile("([^,]*), ([^,]*)");

	@Override
	protected String[] splitStationName(final String name)
	{
		final Matcher mComma = P_SPLIT_NAME_ONE_COMMA.matcher(name);
		if (mComma.matches())
			return new String[] { mComma.group(2), mComma.group(1) };

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

	private static final Pattern P_NORMALIZE_LINE_NAME_TRAM = Pattern.compile("str\\s+(.*)", Pattern.CASE_INSENSITIVE);

	@Override
	protected String normalizeLineName(final String lineName)
	{
		final Matcher mTram = P_NORMALIZE_LINE_NAME_TRAM.matcher(lineName);
		if (mTram.matches())
			return mTram.group(1);

		return super.normalizeLineName(lineName);
	}

	@Override
	protected char normalizeType(String type)
	{
		final String ucType = type.toUpperCase();

		if ("MT".equals(ucType)) // Schnee-Express
			return 'I';

		if ("DZ".equals(ucType)) // Dampfzug
			return 'R';

		if ("LTT".equals(ucType))
			return 'B';

		if (ucType.startsWith("RFB")) // Rufbus
			return 'P';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		if ("E".equals(ucType))
			return '?';

		return 0;
	}
}
