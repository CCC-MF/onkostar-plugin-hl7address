/*
 * MIT License
 *
 * Copyright (c) 2024 Comprehensive Cancer Center Mainfranken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.ukw.ccc.onkostar.hl7address;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.parser.PipeParser;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Patient;
import de.itc.onkostar.api.analysis.AnalyzerRequirement;
import de.itc.onkostar.api.analysis.IHl7Analyzer;
import de.itc.onkostar.api.analysis.OnkostarPluginType;
import de.itc.onkostar.api.hl7.*;
import de.itc.onkostar.api.hl7.wrapper.CX;
import de.itc.onkostar.api.hl7.wrapper.XAD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class Hl7AddressPlugin implements IHl7Analyzer {

    private Logger logger = LoggerFactory.getLogger(Hl7AddressPlugin.class);

    private final IOnkostarApi onkostarApi;

    private final Hl7AddressSplitter hl7AddressSplitter;

    public Hl7AddressPlugin(final IOnkostarApi onkostarApi, final Hl7AddressSplitter hl7AddressSplitter) {
        this.onkostarApi = onkostarApi;
        this.hl7AddressSplitter = hl7AddressSplitter;
    }

    @Override
    public OnkostarPluginType getType() {
        return OnkostarPluginType.HL7;
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getName() {
        return "Onkostar Plugin HL7 Address";
    }

    @Override
    public String getDescription() {
        return "Dieses Onkostar-Plugin ermöglicht das Aufteilen von HL7 Adressen in die einzelnen Bestandteile";
    }

    @Override
    public boolean isRelevantForAnalyzer(Hl7Message hl7Message) {
        return true;
    }

    @Override
    public boolean isSynchronous() {
        return false;
    }

    @Override
    public AnalyzerRequirement getRequirement() {
        return AnalyzerRequirement.HL7;
    }

    @Override
    public void analyze(Hl7Message hl7Message) {
        try {
            var patientOption = getRelatedPatient(hl7Message);
            if (patientOption.isPresent()) {
                getAddressList(hl7Message).forEach((address) -> {
                    var patient = patientOption.get();
                    var patientAddress = patient.getAddress();
                    var hl7Address = hl7AddressSplitter.split(address);

                    if (
                            null != patientAddress && null != patientAddress.getStreet() && null != patientAddress.getHouseNumber() &&
                                    (patientAddress.getStreet().equals(hl7Address.getStreetName())
                                            || patientAddress.getHouseNumber().equals(hl7Address.getHouseNumber()))
                    ) {
                        return;
                    }

                    patientAddress.setStreet(hl7Address.getStreetName());
                    patientAddress.setHouseNumber(hl7Address.getHouseNumber());
                    patient.setAddress(patientAddress);
                    onkostarApi.savePatient(patient);
                });
            }
        } catch (HL7Exception e) {
            logger.error("Kann HL7 Nachricht nicht verarbeiten", e);
        }

    }

    private Optional<Patient> getRelatedPatient(Hl7Message hl7Message) throws HL7Exception {
        PipeParser pipeParser = new PipeParser();
        var message = pipeParser.parse(hl7Message.getMessage());
        var pidStructure = message.get("PID");

        Optional<CX> patientId = null;

        switch (HL7VersionEnum.getHl7Version(hl7Message.getHl7Version())) {
            case V2_3:
                patientId = Arrays.stream(Wrapper2_3.wrap((ca.uhn.hl7v2.model.v23.segment.PID) pidStructure).getPatientIDInternalID()).findFirst();
                break;
            case V2_4:
                patientId = Arrays.stream(Wrapper2_4.wrap((ca.uhn.hl7v2.model.v24.segment.PID) pidStructure).getPatientIDInternalID()).findFirst();
                break;
            case V2_5:
                patientId = Arrays.stream(Wrapper2_5.wrap((ca.uhn.hl7v2.model.v25.segment.PID) pidStructure).getPatientIDInternalID()).findFirst();
                break;
            case V2_6:
                patientId = Arrays.stream(Wrapper2_6.wrap((ca.uhn.hl7v2.model.v26.segment.PID) pidStructure).getPatientIDInternalID()).findFirst();
                break;
        }

        if (patientId.isPresent()) {
            var patient = this.onkostarApi.getPatient(patientId.get().getID().getValue());
            if (null != patient) {
                logger.info("Patient: {}", patient);
                return Optional.of(patient);
            }
            return Optional.empty();
        }

        logger.info("Keine passende HL7 Nachricht mit Struktur 'PID'");

        return Optional.empty();
    }

    private List<String> getAddressList(Hl7Message hl7Message) throws HL7Exception {
        PipeParser pipeParser = new PipeParser();
        var message = pipeParser.parse(hl7Message.getMessage());
        var pidStructure = message.get("PID");
        switch (HL7VersionEnum.getHl7Version(hl7Message.getHl7Version())) {
            case V2_3:
                return Arrays.stream(((ca.uhn.hl7v2.model.v23.segment.PID) pidStructure).getPatientAddress()).map((xad -> {
                            try {
                                return xad.encode();
                            } catch (HL7Exception e) {
                                return null;
                            }
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            case V2_4:
                return Arrays.stream(((ca.uhn.hl7v2.model.v24.segment.PID) pidStructure).getPatientAddress()).map((xad -> {
                            try {
                                return xad.encode();
                            } catch (HL7Exception e) {
                                return null;
                            }
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            case V2_5:
                return Arrays.stream(((ca.uhn.hl7v2.model.v25.segment.PID) pidStructure).getPatientAddress()).map((xad -> {
                            try {
                                return xad.encode();
                            } catch (HL7Exception e) {
                                return null;
                            }
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            case V2_6:
                return Arrays.stream(((ca.uhn.hl7v2.model.v26.segment.PID) pidStructure).getPatientAddress()).map((xad -> {
                            try {
                                return xad.encode();
                            } catch (HL7Exception e) {
                                return null;
                            }
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        }

        logger.info("Keine passende HL7 Nachricht mit Struktur 'PID'");

        return List.of();
    }


}
