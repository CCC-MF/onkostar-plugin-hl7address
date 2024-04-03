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

import java.util.regex.Pattern;

/// See: https://hl7.eu/refactored/dtXAD.html
public class Address {
    private String streetAddress;
    private String otherDesignation;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    protected Address() {
        this.streetAddress = "";
        this.otherDesignation = "";
        this.city = "";
        this.state = "";
        this.postalCode = "";
        this.country = "";
    }

    public static Address.Builder builder() {
        return new Address.Builder();
    }

    public String getStreetAddress() {
        return getStreetAddressFromSapMciFormat(streetAddress);
    }

    public String getOtherDesignation() {
        return otherDesignation;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public String getStreetName() {
        return getStreetNameFromStreetAddress(this.streetAddress);
    }

    public String getHouseNumber() {
        return getHouseNumberFromStreetAddress(this.streetAddress);
    }

    public static String getStreetNameFromStreetAddress(String streetAddress) {
        var pattern = Pattern.compile("(?<streetName>[^,]+)+[,\\s]+(?<houseNumber>([0-9]+[A-Za-z\\s\\-/]*)*)$");
        var matcher = pattern.matcher(getStreetAddressFromSapMciFormat(streetAddress));
        if (matcher.find()) {
            return matcher.group("streetName");
        }
        return streetAddress;
    }

    public static String getHouseNumberFromStreetAddress(String streetAddress) {
        var pattern = Pattern.compile("(?<streetName>[^,]+)+[,\\s]+(?<houseNumber>([0-9]+[A-Za-z\\s\\-/]*)*)$");
        var matcher = pattern.matcher(getStreetAddressFromSapMciFormat(streetAddress));
        if (matcher.find()) {
            return matcher.group("houseNumber");
        }
        return "";
    }

    private static String getStreetAddressFromSapMciFormat(String input) {
        if (input.contains("&")) {
            var parts = input.split("&");
            if (parts.length == 3 && parts[0].equals(String.format("%s %s", parts[1], parts[2]))) {
                return parts[0];
            }
        }

        return input;
    }

    public static class Builder {

        protected Address instance;

        public Builder() {
            this.instance = new Address();
        }

        public Address build() {
            Address result;
            result = this.instance;
            this.instance = null;
            return result;
        }

        public Builder withStreetAddress(String streetAddress) {
            this.instance.streetAddress = streetAddress;
            return this;
        }

        public Builder withOtherDesignation(String otherDesignation) {
            this.instance.otherDesignation = otherDesignation;
            return this;
        }

        public Builder withCity(String city) {
            this.instance.city = city;
            return this;
        }

        public Builder withState(String state) {
            this.instance.state = state;
            return this;
        }

        public Builder withPostalCode(String postalCode) {
            this.instance.postalCode = postalCode;
            return this;
        }

        public Builder withCountry(String country) {
            this.instance.country = country;
            return this;
        }

    }

}
