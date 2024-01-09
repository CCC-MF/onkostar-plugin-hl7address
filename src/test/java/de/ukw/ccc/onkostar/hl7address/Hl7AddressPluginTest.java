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

import de.itc.onkostar.api.Address;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Patient;
import de.itc.onkostar.api.hl7.Hl7Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Hl7AddressPluginTest {

    private IOnkostarApi onkostarApi;

    private Hl7AddressPlugin plugin;

    @BeforeEach
    void setup(
            @Mock IOnkostarApi onkostarApi
    ) {
        this.onkostarApi = onkostarApi;
        this.plugin = new Hl7AddressPlugin(onkostarApi, new DefaultHl7AddressSplitter());
    }

    @Test
    void shouldSaveSplittedAddress() {
        doAnswer(invocationOnMock -> dummyPatient(invocationOnMock.getArgument(0), "Teststraße", "1")).when(onkostarApi).getPatient(anyString());

        plugin.analyze(dummyHl7Message(1));

        var captor = ArgumentCaptor.forClass(Patient.class);
        verify(onkostarApi, times(1)).savePatient(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getAddress().getStreet()).isEqualTo("Testweg");
        assertThat(captor.getValue().getAddress().getHouseNumber()).isEqualTo("42");
    }

    // @see: https://github.com/CCC-MF/onkostar-plugin-hl7address/issues/1
    @Test
    void shouldSaveSplittedAddressWithExistingSplittedAddressIssue1() {
        // Mock existing patient
        doAnswer(invocationOnMock -> dummyPatient(invocationOnMock.getArgument(0), "Am Breitenstein", "25")).when(onkostarApi).getPatient(anyString());

        // HL7 Message with new address: Am Schlag 4
        plugin.analyze(dummyHl7Message(2));

        // Verify new address is saved
        var captor = ArgumentCaptor.forClass(Patient.class);
        verify(onkostarApi, times(1)).savePatient(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getAddress().getStreet()).isEqualTo("Am Schlag");
        assertThat(captor.getValue().getAddress().getHouseNumber()).isEqualTo("4");
    }

    private Hl7Message dummyHl7Message(int id) {
        try {
            var message = new String(new ClassPathResource(String.format("testhl7-%d.hl7", id)).getInputStream().readAllBytes());
            var result = new Hl7Message(onkostarApi);
            result.setHl7Version("2.3");
            result.setMessage(message);
            return result;
        } catch (IOException e) {
            return null;
        }
    }

    private Patient dummyPatient(String patientId, String street, String houseNumber) {
        var address = new Address();
        address.setStreet(street);
        address.setHouseNumber(houseNumber);
        address.setZipCode("012345");
        address.setCity("Musterhausen");

        var result = new Patient(onkostarApi);
        result.setId(42);
        result.setPatientId(patientId);
        result.setGivenName("Patrick");
        result.setFamilyName("Tester");
        result.setAddress(address);
        return result;
    }

}
