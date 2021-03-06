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

import java.util.regex.Matcher;

import de.schildbach.pte.dto.Product;

/**
 * @author Andreas Schildbach
 */
public class LuProvider extends AbstractHafasProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.LU;
	private static final String API_BASE = "http://mobiliteitszentral.hafas.de/hafas/";

	public LuProvider()
	{
		super(API_BASE + "stboard.exe/fn", API_BASE + "ajax-getstop.exe/fn", API_BASE + "query.exe/fn", 9, UTF_8);
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
			return 'B';
		if (value == 128)
			return 'B';
		if (value == 256)
			return 'B';

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
		}
		else if (product == Product.TRAM)
		{
		}
		else if (product == Product.BUS)
		{
			productBits.setCharAt(5, '1');
			productBits.setCharAt(6, '1');
			productBits.setCharAt(7, '1');
			productBits.setCharAt(8, '1');
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
	protected String[] splitStationName(final String name)
	{
		final Matcher mComma = P_SPLIT_NAME_FIRST_COMMA.matcher(name);
		if (mComma.matches())
			return new String[] { mComma.group(1), mComma.group(2) };

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
	protected char normalizeType(final String type)
	{
		final String ucType = type.toUpperCase();

		if ("CRE".equals(ucType))
			return 'R';

		if ("CITYBUS".equals(ucType))
			return 'B';
		if ("NIGHTBUS".equals(ucType))
			return 'B';
		if ("DIFFBUS".equals(ucType))
			return 'B';
		if ("NAVETTE".equals(ucType))
			return 'B';

		final char t = super.normalizeType(type);
		if (t != 0)
			return t;

		return 0;
	}
}
