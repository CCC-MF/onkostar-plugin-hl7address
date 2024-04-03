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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultHL7AddressSplitterTest {

    private final Hl7AddressSplitter addressSplitter = new DefaultHl7AddressSplitter();

    @Test
    public void testShouldSplitWholeHl7Address() {
        var hl7Address = "100 Morgen Weg 16A^^Arnstein^^06456^DE";
        var address = addressSplitter.split(hl7Address);

        assertEquals("100 Morgen Weg 16A", address.getStreetAddress());
        assertEquals("", address.getOtherDesignation());
        assertEquals("Arnstein", address.getCity());
        assertEquals("", address.getState());
        assertEquals("06456", address.getPostalCode());
        assertEquals("DE", address.getCountry());
        assertEquals("100 Morgen Weg", address.getStreetName());
        assertEquals("16A", address.getHouseNumber());
    }

    @ParameterizedTest
    @CsvFileSource(files = "src/test/resources/testdaten.csv", numLinesToSkip = 1)
    public void testShouldSplitStreetNameAndHouseNumber(String hl7Address, String streetName, String houseNumber) {
        var address = addressSplitter.split(hl7Address);

        assertEquals(streetName, address.getStreetName());
        assertEquals(houseNumber, address.getHouseNumber());
    }

    @Test
    public void testShouldSupportSapMciFormat() {
        var hl7Address = "Muster Weg 1&Muster Weg&1^^Musterhausen^^12345^DE^C";
        var address = addressSplitter.split(hl7Address);

        assertEquals("Muster Weg 1", address.getStreetAddress());
        assertEquals("", address.getOtherDesignation());
        assertEquals("Musterhausen", address.getCity());
        assertEquals("", address.getState());
        assertEquals("12345", address.getPostalCode());
        assertEquals("DE", address.getCountry());
        assertEquals("Muster Weg", address.getStreetName());
        assertEquals("1", address.getHouseNumber());
    }
}
