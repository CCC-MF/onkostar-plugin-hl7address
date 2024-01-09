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
import de.itc.onkostar.api.Disease;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReorgAddressPluginTest {

    private IOnkostarApi onkostarApi;

    private ReorgAddressPlugin plugin;

    @BeforeEach
    void setup(
            @Mock IOnkostarApi onkostarApi
    ) {
        this.onkostarApi = onkostarApi;
        this.plugin = new ReorgAddressPlugin(onkostarApi);
    }

    @ParameterizedTest
    @MethodSource("saveTestSource")
    void shouldSaveSplittedAddress(
            String street,
            String houseNumber,
            String expectedStreet,
            String expectedHouseNumber
    ) {
        var patient = dummyPatient(street, houseNumber);

        when(onkostarApi.getPatient(anyInt())).thenReturn(patient);

        plugin.analyze(null, dummyDisease(patient));

        var captor = ArgumentCaptor.forClass(Patient.class);
        verify(onkostarApi, times(1)).savePatient(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getAddress().getStreet()).isEqualTo(expectedStreet);
        assertThat(captor.getValue().getAddress().getHouseNumber()).isEqualTo(expectedHouseNumber);
    }

    @ParameterizedTest
    @MethodSource("ignoreTestSource")
    void shouldIgnoreAddressSplitting(
            String street,
            String houseNumber
    ) {
        var patient = dummyPatient(street, houseNumber);

        when(onkostarApi.getPatient(anyInt())).thenReturn(patient);

        plugin.analyze(null, dummyDisease(patient));

        verify(onkostarApi, never()).savePatient(any(Patient.class));
    }

    // @see: https://github.com/CCC-MF/onkostar-plugin-hl7address/issues/1
    @Test
    void shouldSaveSplittedAddressWithExistingSplittedAddressIssue1() {
        var patient = dummyPatient("Am Schlag 4", "25");

        when(onkostarApi.getPatient(anyInt())).thenReturn(patient);

        plugin.analyze(null, dummyDisease(patient));

        var captor = ArgumentCaptor.forClass(Patient.class);
        verify(onkostarApi, times(1)).savePatient(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getAddress().getStreet()).isEqualTo("Am Schlag");
        assertThat(captor.getValue().getAddress().getHouseNumber()).isEqualTo("4");
    }

    private Patient dummyPatient(String street, String houseNumber) {
        var address = new Address();
        address.setStreet(street);
        address.setHouseNumber(houseNumber);
        address.setZipCode("01234");
        address.setCity("Musterhausen");

        var result = new Patient(onkostarApi);
        result.setId(1);
        result.setPatientId("2000123456");
        result.setGivenName("Patrick");
        result.setFamilyName("Tester");
        result.setAddress(address);
        return result;
    }

    private Disease dummyDisease(Patient patient) {
        var result = new Disease(onkostarApi);
        result.setPatientId(patient.getId());
        return result;
    }

    private static Stream<Arguments> saveTestSource() {
        return Stream.of(
                Arguments.of("Teststraße 42", "", "Teststraße", "42"),
                Arguments.of("Teststraße 42", null, "Teststraße", "42"),
                Arguments.of("Teststraße 42", "42", "Teststraße", "42"),
                Arguments.of("Teststraße 42 ", "  ", "Teststraße", "42"),
                // @see: https://github.com/CCC-MF/onkostar-plugin-hl7address/issues/1#issuecomment-1882282507
                Arguments.of("Am Schlag 4", "35", "Am Schlag", "4")
        );
    }

    private static Stream<Arguments> ignoreTestSource() {
        return Stream.of(
                Arguments.of("Teststraße", "42"),
                Arguments.of("Teststraße ", "42 ")
        );
    }

}
