package com.riskscoring.enrichment.client;

import com.riskscoring.enrichment.client.dto.OfacDigitalCurrencyAddress;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OfacSdnParser {

    private static final String DIGITAL_CURRENCY_PREFIX = "Digital Currency Address - ";

    private static final String FEATURE_TYPE = "FeatureType";
    private static final String DISTINCT_PARTY = "DistinctParty";
    private static final String ALIAS = "Alias";
    private static final String DOCUMENTED_NAME = "DocumentedName";
    private static final String NAME_PART_VALUE = "NamePartValue";
    private static final String FEATURE = "Feature";
    private static final String VERSION_DETAIL = "VersionDetail";

    private static final String ID_ATTR = "ID";
    private static final String FEATURE_TYPE_ID_ATTR = "FeatureTypeID";
    private static final String PRIMARY_ATTR = "Primary";

    public List<OfacDigitalCurrencyAddress> parse(InputStream xml) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(xml);
            try {
                return walk(reader);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new OfacSdnParseException(e);
        }
    }

    private List<OfacDigitalCurrencyAddress> walk(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> tickersByFeatureTypeId = new HashMap<>();
        List<OfacDigitalCurrencyAddress> results = new ArrayList<>();

        PartyState party = new PartyState();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                handleStart(reader, tickersByFeatureTypeId, results, party);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                handleEnd(reader.getLocalName(), party);
            }
        }

        return results;
    }

    private void handleStart(XMLStreamReader reader,
                              Map<String, String> tickersByFeatureTypeId,
                              List<OfacDigitalCurrencyAddress> results,
                              PartyState party) throws XMLStreamException {
        switch (reader.getLocalName()) {
            case FEATURE_TYPE -> registerFeatureType(reader, tickersByFeatureTypeId);
            case DISTINCT_PARTY -> party.reset();
            case ALIAS -> party.inPrimaryAlias = "true".equals(reader.getAttributeValue(null, PRIMARY_ATTR));
            case DOCUMENTED_NAME -> party.inPrimaryDocumentedName = party.inPrimaryAlias && party.name == null;
            case NAME_PART_VALUE -> appendNamePart(reader, party);
            case FEATURE -> party.pendingTicker =
                    tickersByFeatureTypeId.get(reader.getAttributeValue(null, FEATURE_TYPE_ID_ATTR));
            case VERSION_DETAIL -> captureAddress(reader, results, party);
            default -> { }
        }
    }

    private void handleEnd(String localName, PartyState party) {
        switch (localName) {
            case DOCUMENTED_NAME -> {
                if (party.inPrimaryDocumentedName && party.name == null) {
                    party.name = party.nameBuilder.toString();
                }
                party.inPrimaryDocumentedName = false;
            }
            case ALIAS -> party.inPrimaryAlias = false;
            case FEATURE -> party.pendingTicker = null;
            default -> { }
        }
    }

    private void registerFeatureType(XMLStreamReader reader, Map<String, String> tickersByFeatureTypeId)
            throws XMLStreamException {
        String id = reader.getAttributeValue(null, ID_ATTR);
        String description = reader.getElementText();
        if (id != null && description.startsWith(DIGITAL_CURRENCY_PREFIX)) {
            tickersByFeatureTypeId.put(id, description.substring(DIGITAL_CURRENCY_PREFIX.length()));
        }
    }

    private void appendNamePart(XMLStreamReader reader, PartyState party) throws XMLStreamException {
        if (!party.inPrimaryDocumentedName) {
            return;
        }
        String text = reader.getElementText();
        if (!party.nameBuilder.isEmpty()) {
            party.nameBuilder.append(' ');
        }
        party.nameBuilder.append(text);
    }

    private void captureAddress(XMLStreamReader reader, List<OfacDigitalCurrencyAddress> results, PartyState party)
            throws XMLStreamException {
        if (party.pendingTicker == null || party.name == null) {
            return;
        }
        String address = reader.getElementText().trim();
        if (!address.isEmpty()) {
            results.add(new OfacDigitalCurrencyAddress(party.pendingTicker, address, party.name));
        }
    }

    private static final class PartyState {
        String name;
        StringBuilder nameBuilder = new StringBuilder();
        boolean inPrimaryAlias;
        boolean inPrimaryDocumentedName;
        String pendingTicker;

        void reset() {
            name = null;
            nameBuilder = new StringBuilder();
            inPrimaryAlias = false;
            inPrimaryDocumentedName = false;
            pendingTicker = null;
        }
    }
}
