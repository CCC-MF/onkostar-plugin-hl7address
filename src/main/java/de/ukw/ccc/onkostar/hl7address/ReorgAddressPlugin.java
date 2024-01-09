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

import de.itc.onkostar.api.Disease;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.itc.onkostar.api.analysis.AnalyseTriggerEvent;
import de.itc.onkostar.api.analysis.AnalyzerRequirement;
import de.itc.onkostar.api.analysis.IProcedureAnalyzer;
import de.itc.onkostar.api.analysis.OnkostarPluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ReorgAddressPlugin implements IProcedureAnalyzer {

    private final Logger logger = LoggerFactory.getLogger(ReorgAddressPlugin.class);

    private final IOnkostarApi onkostarApi;

    public ReorgAddressPlugin(final IOnkostarApi onkostarApi) {
        this.onkostarApi = onkostarApi;
    }

    @Override
    public OnkostarPluginType getType() {
        return OnkostarPluginType.ANALYZER;
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getName() {
        return "Onkostar Plugin HL7 Address - ReOrg";
    }

    @Override
    public String getDescription() {
        return "Ermöglicht das automatische Aufteilen von Straße und Hausnummer";
    }

    @Override
    public boolean isRelevantForDeletedProcedure() {
        return false;
    }

    @Override
    public boolean isRelevantForAnalyzer(Procedure procedure, Disease disease) {
        return null != disease;
    }

    @Override
    public boolean isSynchronous() {
        return false;
    }

    @Override
    public AnalyzerRequirement getRequirement() {
        return AnalyzerRequirement.DISEASE;
    }

    @Override
    public Set<AnalyseTriggerEvent> getTriggerEvents() {
        return Set.of(AnalyseTriggerEvent.REORG);
    }

    @Override
    public void analyze(Procedure procedure, Disease disease) {
        var patient = disease.getPatient();
        var address = patient.getAddress();

        if (null == address || null == address.getStreet()) {
            logger.warn("Keine vollständige Adresse für Patient '{}'", patient.getPatientId());
            return;
        }

        var street = null == address.getStreet() ? "" : address.getStreet().trim();

        // Case: No HouseNumber within StreetAddress
        if (Address.getHouseNumberFromStreetAddress(street).isBlank()) {
            return;
        }

        address.setStreet(Address.getStreetNameFromStreetAddress(street));
        address.setHouseNumber(Address.getHouseNumberFromStreetAddress(street));

        patient.setAddress(address);
        onkostarApi.savePatient(patient);
    }
}
