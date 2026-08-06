package com.riskscoring.enrichment.client;

import com.riskscoring.enrichment.client.dto.OfacDigitalCurrencyAddress;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OfacSdnParserTest {

    private final OfacSdnParser parser = new OfacSdnParser();

    @Test
    void extractsDigitalCurrencyAddressesWithEntityNameAndSkipsNonDigitalCurrencyFeatures() {
        List<OfacDigitalCurrencyAddress> result = parser.parse(fixture());

        assertThat(result).containsExactlyInAnyOrder(
                new OfacDigitalCurrencyAddress("ETH", "0x098B716B8Aaf21512996dC57EB0615e2383E2f96", "Lazarus Group"),
                new OfacDigitalCurrencyAddress("XMR", "44AFFq5kSiGBoZ4NMDwYtN18obc8AemS33DBLWs3H7otXft3XjrpDtQGv7SqSsaBYBb98uNbr2VBBEt7f2wfn3RVGQBEP3A",
                        "John Doe"));
    }

    private InputStream fixture() {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <Sanctions xmlns="https://sanctionslistservice.ofac.treas.gov/api/PublicationPreview/exports/ADVANCED_XML">
                  <ReferenceValueSets>
                    <FeatureTypeValues>
                      <FeatureType ID="1">Vessel Call Sign</FeatureType>
                      <FeatureType ID="345">Digital Currency Address - ETH</FeatureType>
                      <FeatureType ID="444">Digital Currency Address - XMR</FeatureType>
                    </FeatureTypeValues>
                  </ReferenceValueSets>
                  <DistinctParties>
                    <DistinctParty FixedRef="27307">
                      <Profile ID="27307" PartySubTypeID="3">
                        <Identity ID="19011" FixedRef="27307" Primary="true">
                          <Alias FixedRef="27307" AliasTypeID="1403" Primary="true" LowQuality="false">
                            <DocumentedName ID="37142" FixedRef="27307" DocNameStatusID="1">
                              <DocumentedNamePart>
                                <NamePartValue NamePartGroupID="72465">Lazarus Group</NamePartValue>
                              </DocumentedNamePart>
                            </DocumentedName>
                          </Alias>
                          <Alias FixedRef="27307" AliasTypeID="1400" Primary="false" LowQuality="true">
                            <DocumentedName ID="37271" FixedRef="27307" DocNameStatusID="2">
                              <DocumentedNamePart>
                                <NamePartValue NamePartGroupID="72499">Hidden Cobra</NamePartValue>
                              </DocumentedNamePart>
                            </DocumentedName>
                          </Alias>
                        </Identity>
                        <Feature ID="1" FeatureTypeID="999">
                          <FeatureVersion ID="1" ReliabilityID="1">
                            <VersionDetail DetailTypeID="1432">irrelevant feature type</VersionDetail>
                          </FeatureVersion>
                          <IdentityReference IdentityID="19011" IdentityFeatureLinkTypeID="1" />
                        </Feature>
                        <Feature ID="50215" FeatureTypeID="345">
                          <FeatureVersion ID="47914" ReliabilityID="1560">
                            <Comment />
                            <VersionDetail DetailTypeID="1432">0x098B716B8Aaf21512996dC57EB0615e2383E2f96</VersionDetail>
                          </FeatureVersion>
                          <IdentityReference IdentityID="19011" IdentityFeatureLinkTypeID="1" />
                        </Feature>
                      </Profile>
                    </DistinctParty>
                    <DistinctParty FixedRef="27308">
                      <Profile ID="27308" PartySubTypeID="4">
                        <Identity ID="19012" FixedRef="27308" Primary="true">
                          <Alias FixedRef="27308" AliasTypeID="1403" Primary="true" LowQuality="false">
                            <DocumentedName ID="37150" FixedRef="27308" DocNameStatusID="1">
                              <DocumentedNamePart>
                                <NamePartValue NamePartGroupID="72470">John</NamePartValue>
                              </DocumentedNamePart>
                              <DocumentedNamePart>
                                <NamePartValue NamePartGroupID="72471">Doe</NamePartValue>
                              </DocumentedNamePart>
                            </DocumentedName>
                          </Alias>
                        </Identity>
                        <Feature ID="50300" FeatureTypeID="444">
                          <FeatureVersion ID="47950" ReliabilityID="1560">
                            <VersionDetail DetailTypeID="1431" DetailReferenceID="91905" />
                          </FeatureVersion>
                          <IdentityReference IdentityID="19012" IdentityFeatureLinkTypeID="1" />
                        </Feature>
                        <Feature ID="50301" FeatureTypeID="444">
                          <FeatureVersion ID="47951" ReliabilityID="1560">
                            <VersionDetail DetailTypeID="1432">44AFFq5kSiGBoZ4NMDwYtN18obc8AemS33DBLWs3H7otXft3XjrpDtQGv7SqSsaBYBb98uNbr2VBBEt7f2wfn3RVGQBEP3A</VersionDetail>
                          </FeatureVersion>
                          <IdentityReference IdentityID="19012" IdentityFeatureLinkTypeID="1" />
                        </Feature>
                      </Profile>
                    </DistinctParty>
                  </DistinctParties>
                </Sanctions>
                """;
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
