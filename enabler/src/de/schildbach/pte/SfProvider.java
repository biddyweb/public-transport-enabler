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

import java.util.HashMap;
import java.util.Map;

import de.schildbach.pte.dto.Style;

/**
 * @author Andreas Schildbach
 */
public class SfProvider extends AbstractEfaProvider
{
	public static final NetworkId NETWORK_ID = NetworkId.SF;
	private final static String API_BASE = "http://tripplanner.transit.511.org/mtc/";

	public SfProvider()
	{
		super(API_BASE);

		setTimeZone("America/Los_Angeles");
		setUseRouteIndexAsTripId(false);
		setFareCorrectionFactor(0.01f);
		setStyles(STYLES);
	}

	public NetworkId id()
	{
		return NETWORK_ID;
	}

	@Override
	protected String normalizeLocationName(final String name)
	{
		if (name == null || name.length() == 0)
			return null;

		return super.normalizeLocationName(name).replace("$XINT$", "&");
	}

	@Override
	protected String parseLine(final String mot, final String symbol, final String name, final String longName, final String trainType,
			final String trainNum, final String trainName)
	{
		if ("0".equals(mot))
		{
			if (("XAA".equals(symbol) || "Daly City / Fremont".equals(symbol)) && "Daly City / Fremont".equals(name))
				return "RDALY/FRMT";
			if (("FRE".equals(symbol) || "Fremont / Daly City".equals(symbol)) && "Fremont / Daly City".equals(name))
				return "RFRMT/DALY";
			if (("XAC".equals(symbol) || "Fremont / Richmond".equals(symbol)) && "Fremont / Richmond".equals(name))
				return "RFRMT/RICH";
			if (("XAD".equals(symbol) || "Richmond / Fremont".equals(symbol)) && "Richmond / Fremont".equals(name))
				return "RRICH/FRMT";
			if (("XAE".equals(symbol) || "Pittsburg Bay Point / SFO".equals(symbol)) && "Pittsburg Bay Point / SFO".equals(name))
				return "RPITT/SFIA";
			if (("SFI".equals(symbol) || "SFO / Pittsburg Bay Point".equals(symbol)) && "SFO / Pittsburg Bay Point".equals(name))
				return "RSFIA/PITT";
			if (("XAF".equals(symbol) || "Millbrae / Richmond".equals(symbol)) && "Millbrae / Richmond".equals(name))
				return "RMLBR/RICH";
			if (("XAG".equals(symbol) || "Richmond / Millbrae".equals(symbol)) && "Richmond / Millbrae".equals(name))
				return "RRICH/MLBR";
			if (("XAH".equals(symbol) || "Daly City / Dublin Pleasanton".equals(symbol)) && "Daly City / Dublin Pleasanton".equals(name))
				return "RDALY/DUBL";
			if (("XAI".equals(symbol) || "Dublin Pleasanton / Daly City".equals(symbol)) && "Dublin Pleasanton / Daly City".equals(name))
				return "RDUBL/DALY";

			if ("LOC".equals(symbol) && "LOCAL".equals(name))
				return "RLocal";
			if ("CAP".equals(symbol) && "CAPITOL".equals(name))
				return "RCapitol";

			if ("Muni Rail".equals(trainName) && symbol != null) // Muni
				return 'T' + symbol;
			if (trainType == null && "F".equals(trainNum)) // Muni Historic Streetcar
				return "TF";
			if (trainType == null && "J".equals(trainNum)) // Muni Metro
				return "TJ";
			if (trainType == null && "K".equals(trainNum)) // Muni Metro
				return "TK";
			if (trainType == null && "KT".equals(trainNum)) // Muni Metro
				return "TKT";
			if (trainType == null && "L".equals(trainNum)) // Muni Metro
				return "TL";
			if (trainType == null && "M".equals(trainNum)) // Muni Metro
				return "TM";
			if (trainType == null && "N".equals(trainNum)) // Muni Metro
				return "TN";
			if (trainType == null && "T".equals(trainNum)) // Muni Metro
				return "TT";
		}

		return super.parseLine(mot, symbol, name, longName, trainType, trainNum, trainName);
	}

	private static final Map<String, Style> STYLES = new HashMap<String, Style>();

	static
	{
		// BART
		STYLES.put("RDALY/FRMT", new Style(Style.parseColor("#4EBF49"), Style.WHITE));
		STYLES.put("RFRMT/DALY", new Style(Style.parseColor("#4EBF49"), Style.WHITE));

		STYLES.put("RFRMT/RICH", new Style(Style.parseColor("#FAA61A"), Style.WHITE));
		STYLES.put("RRICH/FRMT", new Style(Style.parseColor("#FAA61A"), Style.WHITE));

		STYLES.put("RSFIA/PITT", new Style(Style.parseColor("#FFE800"), Style.BLACK));
		STYLES.put("RPITT/SFIA", new Style(Style.parseColor("#FFE800"), Style.BLACK));

		STYLES.put("RMLBR/RICH", new Style(Style.parseColor("#F81A23"), Style.WHITE));
		STYLES.put("RRICH/MLBR", new Style(Style.parseColor("#F81A23"), Style.WHITE));

		STYLES.put("RDALY/DUBL", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
		STYLES.put("RDUBL/DALY", new Style(Style.parseColor("#00AEEF"), Style.WHITE));
	}
}
