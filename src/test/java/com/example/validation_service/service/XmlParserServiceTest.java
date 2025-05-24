package com.example.validation_service.service;

import com.example.validation_service.dto.RawPaymentData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XmlParserServiceTest {

    private XmlParserService xmlParserService;

    @BeforeEach
    void setUp() {
        xmlParserService = new XmlParserService();
    }

    private String createValidXml(String msgId, String instrId, String endToEndId, String debtorName, String debtorId, String creditorName, String creditorId, String amount, String currency) {
        return String.format(
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">" +
            "  <FIToFICstmrCdtTrf>" +
            "    <GrpHdr>" +
            "      <MsgId>%s</MsgId>" +
            "      <CreDtTm>2023-10-26T10:00:00</CreDtTm>" +
            "    </GrpHdr>" +
            "    <CdtTrfTxInf>" +
            "      <PmtId>" +
            "        <InstrId>%s</InstrId>" +
            "        <EndToEndId>%s</EndToEndId>" +
            "      </PmtId>" +
            "      <InstdAmt Ccy=\"%s\">%s</InstdAmt>" +
            "      <Dbtr>" +
            "        <Nm>%s</Nm>" +
            "        <PstlAdr/>" + // Adding some other elements to make it more realistic
            "        <Id><OrgId><AnyBIC>%s</AnyBIC></OrgId></Id>" +
            "      </Dbtr>" +
            "      <Cdtr>" +
            "        <Nm>%s</Nm>" +
            "        <Id><OrgId><AnyBIC>%s</AnyBIC></OrgId></Id>" +
            "      </Cdtr>" +
            "    </CdtTrfTxInf>" +
            "  </FIToFICstmrCdtTrf>" +
            "</Document>",
            msgId, instrId, endToEndId, currency, amount, debtorName, debtorId, creditorName, creditorId
        );
    }

    @Test
    void testParse_Success() throws XmlParsingException {
        String xml = createValidXml("MSG001", "INSTR001", "ENDTOEND001", "Debtor Name", "DEBTORID", "Creditor Name", "CREDITORID", "123.45", "USD");
        RawPaymentData result = xmlParserService.parse(xml);

        assertNotNull(result);
        assertEquals("MSG001", result.getMsgId());
        assertEquals("INSTR001", result.getInstrId());
        assertEquals("ENDTOEND001", result.getEndToEndId());
        assertEquals("Debtor Name", result.getDebtorName());
        assertEquals("DEBTORID", result.getDebtorId());
        assertEquals("Creditor Name", result.getCreditorName());
        assertEquals("CREDITORID", result.getCreditorId());
        assertEquals("123.45", result.getAmount());
        assertEquals("USD", result.getCurrency());
    }

    @Test
    void testParse_MalformedXml_ThrowsXmlParsingException() {
        String xml = "<Document><UnclosedTag>Test</Document>";
        XmlParsingException exception = assertThrows(XmlParsingException.class, () -> {
            xmlParserService.parse(xml);
        });
        assertTrue(exception.getMessage().contains("Error parsing XML"));
    }

    @Test
    void testParse_MissingRequiredElement_MsgId_ThrowsXmlParsingException() {
        String xml = createValidXml(null, "INSTR001", "ENDTOEND001", "Debtor Name", "DEBTORID", "Creditor Name", "CREDITORID", "123.45", "USD")
            .replace("<MsgId null=\"null\" />", ""); // Attempt to remove MsgId, careful with null in format

        // A better way to create XML without MsgId
         String xmlWithoutMsgId =
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">" +
            "  <FIToFICstmrCdtTrf>" +
            "    <GrpHdr>" +
            // "      <MsgId>MSG001</MsgId>" + // MsgId is removed
            "      <CreDtTm>2023-10-26T10:00:00</CreDtTm>" +
            "    </GrpHdr>" +
            "    <CdtTrfTxInf>" +
            "      <PmtId>" +
            "        <InstrId>INSTR001</InstrId>" +
            "        <EndToEndId>ENDTOEND001</EndToEndId>" +
            "      </PmtId>" +
            "      <InstdAmt Ccy=\"USD\">123.45</InstdAmt>" +
            "      <Dbtr><Nm>Debtor Name</Nm><Id><OrgId><AnyBIC>DEBTORID</AnyBIC></OrgId></Id></Dbtr>" +
            "      <Cdtr><Nm>Creditor Name</Nm><Id><OrgId><AnyBIC>CREDITORID</AnyBIC></OrgId></Id></Cdtr>" +
            "    </CdtTrfTxInf>" +
            "  </FIToFICstmrCdtTrf>" +
            "</Document>";


        XmlParsingException exception = assertThrows(XmlParsingException.class, () -> {
            xmlParserService.parse(xmlWithoutMsgId);
        });
        assertTrue(exception.getMessage().contains("Parsed XML is missing essential fields"));
    }
    
    @Test
    void testParse_MissingRequiredElement_Amount_ThrowsXmlParsingException() {
         String xmlWithoutAmount =
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">" +
            "  <FIToFICstmrCdtTrf>" +
            "    <GrpHdr>" +
            "      <MsgId>MSG001</MsgId>" +
            "      <CreDtTm>2023-10-26T10:00:00</CreDtTm>" +
            "    </GrpHdr>" +
            "    <CdtTrfTxInf>" +
            "      <PmtId>" +
            "        <InstrId>INSTR001</InstrId>" +
            "        <EndToEndId>ENDTOEND001</EndToEndId>" +
            "      </PmtId>" +
            // "      <InstdAmt Ccy=\"USD\">123.45</InstdAmt>" + // Amount is removed
            "      <Dbtr><Nm>Debtor Name</Nm><Id><OrgId><AnyBIC>DEBTORID</AnyBIC></OrgId></Id></Dbtr>" +
            "      <Cdtr><Nm>Creditor Name</Nm><Id><OrgId><AnyBIC>CREDITORID</AnyBIC></OrgId></Id></Cdtr>" +
            "    </CdtTrfTxInf>" +
            "  </FIToFICstmrCdtTrf>" +
            "</Document>";

        XmlParsingException exception = assertThrows(XmlParsingException.class, () -> {
            xmlParserService.parse(xmlWithoutAmount);
        });
        assertTrue(exception.getMessage().contains("Parsed XML is missing essential fields"));
    }


    @Test
    void testParse_EmptyOptionalElement_StoresNullOrEmpty() throws XmlParsingException {
        // InstrId is optional in RawPaymentData, but not essential for the parser to throw error
        // Let's test with an empty DebtorId which is also optional in terms of basic parsing
        String xml = createValidXml("MSG001", "INSTR001", "ENDTOEND001", "Debtor Name", "", "Creditor Name", "CREDITORID", "123.45", "USD");
        RawPaymentData result = xmlParserService.parse(xml);
        // XmlParserService stores empty string as null for some fields, let's verify this behavior
        assertNull(result.getDebtorId()); // Or assertEquals("", result.getDebtorId()); depending on impl.
                                          // Current XmlParserService trims and if empty, doesn't set, so it remains null.
    }

    @Test
    void testParse_AmountWithDecimals_CorrectlyExtracted() throws XmlParsingException {
        String xml = createValidXml("MSG001", "INSTR001", "ENDTOEND001", "Debtor Name", "DEBTORID", "Creditor Name", "CREDITORID", "5000.75", "EUR");
        RawPaymentData result = xmlParserService.parse(xml);
        assertEquals("5000.75", result.getAmount());
    }
    
    @Test
    void testParse_AmountWithoutDecimals_CorrectlyExtracted() throws XmlParsingException {
        String xml = createValidXml("MSG001", "INSTR001", "ENDTOEND001", "Debtor Name", "DEBTORID", "Creditor Name", "CREDITORID", "5000", "EUR");
        RawPaymentData result = xmlParserService.parse(xml);
        assertEquals("5000", result.getAmount());
    }


    @Test
    void testParse_XXEVulnerability_Safe() {
        // This test is more conceptual for StAX.
        // We've configured XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES to false and XMLInputFactory.SUPPORT_DTD to false.
        // A real XXE payload would try to access external files or network resources.
        // StAX parsers, when configured correctly, should prevent this.
        // This test primarily serves as a reminder and documentation of this configuration.
        String xxePayload = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><data>&xxe;</data>";
         // If XMLInputFactory.SUPPORT_DTD was true, this might throw different errors or try to resolve entity.
         // With it false, it should typically lead to a parsing error related to DTD not supported or entity not resolved.
        XmlParsingException exception = assertThrows(XmlParsingException.class, () -> {
            xmlParserService.parse(xxePayload);
        });
        // The exact message might vary, but it should indicate a parsing failure
        // not a successful read of /etc/passwd.
        assertTrue(exception.getMessage().contains("Error parsing XML"));
        // We cannot directly assert that /etc/passwd was not read, but the parsing failure of the XXE structure is an indicator.
    }
}
