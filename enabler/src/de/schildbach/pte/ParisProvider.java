/*
 * Copyright 2014-2015 the original author or authors.
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

import de.schildbach.pte.dto.Style;
import de.schildbach.pte.dto.Style.Shape;

/**
 * @author Antonio El Khoury
 */
public class ParisProvider extends AbstractNavitiaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.PARIS;
	private static String API_REGION = "fr-idf";

	public ParisProvider(final String authorization)
	{
		super(authorization);

		setTimeZone("Europe/Paris");
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	public String region()
	{
		return API_REGION;
	}

	@Override
	protected Style getLineStyle(final char product, final String code, final String color)
	{
		switch (product)
		{
			case 'S':
			{
				// RER
				if (code.compareTo("F") < 0)
				{
					return new Style(Shape.CIRCLE, Style.TRANSPARENT, Style.parseColor(color), Style.parseColor(color));
				}
				// Transilien
				else
				{
					return new Style(Shape.ROUNDED, Style.TRANSPARENT, Style.parseColor(color), Style.parseColor(color));
				}
			}
			case 'U':
			{
				// Metro
				return new Style(Shape.CIRCLE, Style.parseColor(color), computeForegroundColor(color));
			}
			case 'T':
			{
				// Tram
				return new Style(Shape.RECT, Style.parseColor(color), computeForegroundColor(color));
			}
			case 'B':
			{
				// Bus + Noctilien
				return new Style(Shape.RECT, Style.parseColor(color), computeForegroundColor(color));
			}
			default:
				throw new IllegalArgumentException("Unhandled product: " + product);
		}
	}
}
