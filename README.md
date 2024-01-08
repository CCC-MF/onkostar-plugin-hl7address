# Onkostar Plugin für HL7 Adressen

Dieses Onkostar-Plugin ermöglicht das Verarbeiten von Adressen in HL7 Nachrichten und das automatische Aufteilen von
Straße und Hausnummer.

## Eigenschaften des Plugins

Das Plugin wird nach der internen Verarbeitung der HL7-Nachricht durch Onkostar ausgeführt.

Das bedeutet, dass bereits immer eine Anschrift, hier zunächst mit Straße und Hausnummer im Formularfeld "Straße" für
einen Patienten vorhanden ist.

Neue Patienten werden im jeweils konfigurierten Personenstamm ebenfalls vor Ausführung des Plugins angelegt und sind
daher bereits in der Datenbank vorhanden.

### Aufteilung in Straße und Hausnummer

In HL7-Nachrichten liegen die Anschriften immer in der folgenden Form vor:

```
<streetAddress>^<otherDesignation>^<city>^<state>^<postalCode>^<country>
```

Das Plugin teilt dabei zunächst die Anschrift in die einzelnen Bestandteile auf.
In HL7 ist eine Aufteilung von Straße und Hausnummer nicht vorgesehen, beide Angaben stehen zusammen im Bereich `<streetAddress>`. 

Die weitere Aufteilung des Bereichs `<streetAddress>` erfolgt mithilfe eines regulären Ausdrucks:

```
(?<streetName>.+)+\s+(?<houseNumber>([0-9]+[A-Za-z\s\-/]*)*)$
```

* `streetName` → Alle Zeichen möglich, gefolgt von einem (oder mehreren) abschließenden Leerzeichen
* `houseNumber` → Der Rest der Zeichenkette bis zum Ende (`$`) muss dabei aus einer (optionalen, falls Hausnummer bereits da fehlt) Folge von:
   * mindestens einer Ziffer
   * gefolgt von jeweils optionalen Buchstaben (ohne Umlaute!), Leerzeichen, Bindestrichen oder Schrägstrichen bestehen

Sollte die Trennung von Straße und Hausnummer fehlschlagen, wird die Zeichenkette als Straße verwendet.

## Tests

Dem Plugin liegt eine Datei mit 500 Testanschriften bei, die für UnitTests verwendet werden.