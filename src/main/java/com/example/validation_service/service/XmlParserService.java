package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;

@Service
public class XmlParserService {

    private static final Logger logger = LoggerFactory.getLogger(XmlParserService.class);

    public RawPaymentData parse(String xmlPayload) throws XmlParsingException {
        RawPaymentData data = new RawPaymentData();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Defend against XXE
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);


        try {
            XMLEventReader eventReader = factory.createXMLEventReader(new StringReader(xmlPayload));
            String currentElement = null;
            boolean inDbtr = false;
            boolean inCdtr = false;
            boolean inInstdAmt = false;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    currentElement = startElement.getName().getLocalPart();

                    if ("Dbtr".equals(currentElement)) {
                        inDbtr = true;
                    } else if ("Cdtr".equals(currentElement)) {
                        inCdtr = true;
                    } else if ("InstdAmt".equals(currentElement)) {
                        inInstdAmt = true;
                        // Extract currency attribute from InstdAmt
                        javax.xml.namespace.QName currencyAttribute = new javax.xml.namespace.QName("Ccy");
                        if (startElement.getAttributeByName(currencyAttribute) != null) {
                             data.setCurrency(startElement.getAttributeByName(currencyAttribute).getValue());
                        }
                    }
                } else if (event.isCharacters()) {
                    Characters characters = event.asCharacters();
                    String text = characters.getData().trim();
                    if (text.isEmpty() || currentElement == null) {
                        continue;
                    }

                    switch (currentElement) {
                        case "MsgId":
                            data.setMsgId(text);
                            break;
                        case "InstrId":
                            data.setInstrId(text);
                            break;
                        case "EndToEndId":
                            data.setEndToEndId(text);
                            break;
                        case "Nm":
                            if (inDbtr) {
                                data.setDebtorName(text);
                            } else if (inCdtr) {
                                data.setCreditorName(text);
                            }
                            break;
                        case "Id": // This is a simplification. Real ISO 20022 might have OrgId/PrvtId/Othr
                                   // For now, we take the first Id we find under Dbtr/Cdtr.
                                   // A more robust solution would check parent elements (e.g. OrgId, PrvtId)
                            if (inDbtr && data.getDebtorId() == null) { // only set if not already set
                                data.setDebtorId(text);
                            } else if (inCdtr && data.getCreditorId() == null) { // only set if not already set
                                data.setCreditorId(text);
                            }
                            break;
                        case "Amt":
                             if(inInstdAmt){
                                data.setAmount(text);
                            }
                            break;
                        // Currency is handled as an attribute of InstdAmt
                    }
                } else if (event.isEndElement()) {
                    String endElement = event.asEndElement().getName().getLocalPart();
                    if ("Dbtr".equals(endElement)) {
                        inDbtr = false;
                    } else if ("Cdtr".equals(endElement)) {
                        inCdtr = false;
                    } else if ("InstdAmt".equals(endElement)) {
                        inInstdAmt = false;
                    }
                    currentElement = null; // Reset current element on end tag
                }
            }
        } catch (XMLStreamException e) {
            logger.error("Failed to parse XML payload: {}", e.getMessage());
            throw new XmlParsingException("Error parsing XML: " + e.getMessage(), e);
        }

        // Basic validation to ensure essential fields are present
        if (data.getMsgId() == null || data.getAmount() == null || data.getCurrency() == null) {
            logger.error("Parsed data is missing essential fields: MsgId, Amount, or Currency.");
            throw new XmlParsingException("Parsed XML is missing essential fields (e.g., MsgId, Amount, Currency).");
        }

        logger.info("Successfully parsed XML to RawPaymentData: {}", data);
        return data;
    }
}
