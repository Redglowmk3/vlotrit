# Privacybeleid VLOTRIT

**Laatst bijgewerkt: 25 november 2026**  
Dit beleid geldt voor VLOTRIT, waaronder de Google Play-versie (`com.vlotrit`) en de GitHub-versie (`nl.ritbon.app`).

## Wie beheert VLOTRIT?

VLOTRIT wordt beheerd door de appontwikkelaar achter **Redglowmk3**. De app heeft geen account, eigen gebruikersdatabase of eigen server waarop ritgegevens worden verzameld. De ontwikkelaar ontvangt je ritten en locatiegegevens niet, behalve wanneer je die zelf deelt, bijvoorbeeld door een e-mail te versturen.

Privacyvragen: [Redglowmk3@gmail.com](mailto:Redglowmk3@gmail.com).

## Welke gegevens verwerkt de app?

### Gegevens die op je apparaat worden bewaard

VLOTRIT kan gegevens bewaren die je zelf invoert of tijdens het gebruik aanmaakt:

- voertuignaam, kenteken, brandstofsoort, gemiddeld verbruik, prijzen en afschrijvingsinstellingen;
- kilometerstanden, ritnaam, datum en tijd, afstand, kosten, privé- of zakelijke categorie en laadbeurten;
- GPS-routepunten, begin- en eindlocaties en routeplannerinvoer wanneer je die functies gebruikt;
- onderhoudsherinneringen en je instellingen.

Deze gegevens staan in de privéopslag van VLOTRIT op je apparaat. De ontwikkelaar kan ze niet op afstand bekijken. Je kunt ritten, laadbeurten, voertuigen en herinneringen in de app verwijderen. Als Android-back-up op je apparaat is ingeschakeld, kan Android appgegevens volgens je apparaat- en back-upinstellingen ook in je apparaatback-up opnemen. Verwijderen uit de app verwijdert geen kopieën die al door een door jou gekozen back-up- of deelservice zijn gemaakt.

### Locatie en GPS

Locatietoegang is nodig voor GPS-ritregistratie, automatische zakelijke ritten en het zoeken naar stations in de buurt. Je kunt voor een rit GPS uitzetten en de afstand handmatig invoeren. Als je een werkdag start, kan VLOTRIT locatie gebruiken om ritten automatisch te herkennen zolang die werkdag actief is. Android toont daarbij de ritmelding.

GPS-routepunten en berekende afstanden worden op je apparaat opgeslagen. Bij het opslaan van een rit kan VLOTRIT de coördinaten van vertrek en aankomst laten omzetten naar een adres via de Geocoder-dienst die op je Android-apparaat beschikbaar is. De gebruikte locatieprovider kan afhankelijk zijn van je apparaat- en Android-instellingen.

### Kenteken en voertuiggegevens

Wanneer je zelf de kentekenzoekfunctie gebruikt, stuurt VLOTRIT het ingevoerde kenteken naar de openbare RDW Open Data-dienst om voertuig-, APK- en verbruiksgegevens op te halen. Het kenteken wordt ook lokaal in je voertuigprofiel bewaard als je het daar opslaat.

### Routeplanner en stations

Als je de routeplanner gebruikt, stuurt VLOTRIT de ingevoerde vertrekplaats, bestemming en tussenstops naar OpenStreetMap Nominatim om coördinaten te vinden. De gevonden routecoördinaten worden naar de openbare OSRM-routeplanner gestuurd om een autoroute te berekenen.

Als je stations in de buurt zoekt, stuurt VLOTRIT je huidige locatiecoördinaten naar een Overpass-dienst om tankstations en laadpunten in de buurt te vinden. Voor plaats- en adresinformatie kan VLOTRIT coördinaten naar OpenStreetMap Nominatim sturen. De stationskaart vraagt kaarttegels op bij OpenFreeMap. Voor stationsprijsinformatie kan de plaatsnaam naar Hoeveelbenzine.nl worden gestuurd. VLOTRIT haalt daarnaast openbare brandstofprijsinformatie op van ANWB; daarvoor wordt geen exacte GPS-route meegestuurd.

Deze diensten ontvangen de gegevens die nodig zijn voor de aangevraagde functie. Hun servers kunnen daarnaast gebruikelijke technische gegevens verwerken, zoals IP-adres, tijdstip en technische aanvraaginformatie. De diensten worden door derden beheerd en vallen onder hun eigen privacy- en bewaarbeleid.

### Updates

Als je VLOTRIT op updates laat controleren, vraagt de app GitHub om informatie over de openbare VLOTRIT-release. Bij het controleren van wijzigingsnotities kan ook een vergelijking tussen versienummers worden opgevraagd. Als je een update downloadt, wordt de APK van GitHub gedownload. GitHub kan daarbij gebruikelijke technische aanvraaggegevens verwerken. De optionele updatewaarschuwing controleert periodiek zolang die instelling aanstaat en Android de taak laat uitvoeren.

### Delen en contact

Een bon, overzicht, afbeelding, PDF of GPX-route wordt alleen klaargezet wanneer je zelf op delen of exporteren tikt. Android laat je daarna een app kiezen. De gekozen app ontvangt de informatie of het bestand dat je deelt en kan die volgens het eigen beleid verwerken.

In Contact vult VLOTRIT een e-mail aan Redglowmk3@gmail.com voor je in. Er wordt niets automatisch verzonden: je e-mailapp toont het bericht en je moet zelf op verzenden tikken. Als je de e-mail verzendt, ontvangt de ontwikkelaar je e-mailadres en de inhoud van het bericht en gebruikt die om je vraag of feedback te beantwoorden.

## Met wie worden gegevens gedeeld?

VLOTRIT verkoopt geen gegevens en gebruikt geen advertentie- of analytics-SDK's. De app deelt alleen gegevens met de dienst die nodig is voor de functie die je op dat moment gebruikt, zoals hierboven beschreven, of met een deelapp die je zelf kiest. De appontwikkelaar ontvangt geen GPS-route-, rit- of voertuiggegevens via een VLOTRIT-server.

De gebruikte externe diensten kunnen onder meer zijn:

- [RDW Open Data](https://opendata.rdw.nl/)
- [OpenStreetMap Nominatim](https://nominatim.openstreetmap.org/) en [OpenStreetMap](https://www.openstreetmap.org/)
- [Overpass-diensten](https://overpass-api.de/) voor stations in de buurt
- [OSRM](https://router.project-osrm.org/) voor routeberekening
- [OpenFreeMap](https://openfreemap.org/) voor kaarttegels
- [ANWB](https://www.anwb.nl/)
- [Hoeveelbenzine.nl](https://hoeveelbenzine.nl/)
- [GitHub](https://github.com/) voor releases en APK-downloads
- Android of de locatie-/Geocoder-provider van je apparaat

## Bewaren en verwijderen

Lokale rit-, voertuig- en laadgegevens blijven bewaard totdat je ze in VLOTRIT verwijdert, de appgegevens wist of de app verwijdert. Android-back-ups en gegevens bij apps die je zelf gebruikt om iets te delen, vallen onder de instellingen en bewaartermijnen van die diensten. E-mails die je verstuurt, blijven in je e-mailaccount en de inbox van de ontvanger totdat ze daar worden verwijderd.

## Beveiliging en keuzes

VLOTRIT gebruikt de afgeschermde opslag van Android en maakt voor online functies gebruik van HTTPS. Je kunt locatietoestemming weigeren, GPS per rit uitzetten, handmatig rijden zonder GPS, en meldingen via Instellingen of Android beheren. Sommige functies, zoals stations in de buurt en automatische GPS-registratie, werken dan niet of zijn beperkt.

## Kinderen en wijzigingen

VLOTRIT is niet specifiek op kinderen gericht en bevat geen advertenties. Dit beleid kan worden aangepast als de app of de gebruikte diensten veranderen. De datum bovenaan geeft aan wanneer het beleid voor het laatst is bijgewerkt.

## Contact en privacyverzoeken

Mail privacyvragen of verzoeken over een e-mail die je aan de ontwikkelaar hebt gestuurd naar [Redglowmk3@gmail.com](mailto:Redglowmk3@gmail.com). Rit- en voertuiggegevens die alleen op je apparaat staan, kan de ontwikkelaar niet op afstand inzien of verwijderen; beheer die via de app of de Android-instellingen.
