# Mission Mars

## Gruppe
Gruppenavn: Null & Void

Navne: Oliver Ellegaard, Sofie Jørgensen, Henriette Larsen, Mathias Lund

## Kort beskrivelse af opgave
Opgaven gik ud på at lave et system med en server der kan forbinde til flere klienter. 
Klienterne skulle forestille sig sensorer der hver tilsender serveren data i form af temperatur, iltindhold, oxygen og lufttryk, i passende intervaller. 
Serveren forestillede en Mars base HQ der skulle logge disse målinger.

## Hvordan systemet fungerer
For at starte programmet skal man køre serveren MarsHQServer og derefter kan man starte SensorKlient. 
I SensorKlient kan man vælge hvilken type måling der skal sendes og derefter sendes disse værdier til serveren hvert 5. sekund.

## Brug af AI
#### En opgave vi gav agenten 
Vi fik agenten til at ændre på vores SensorLog, så den havde en simplere version der passede med det vi havde arbejdet med før,
og ikke en advanceret version som vi kendte til.

#### Hvorfor opgaven var afgrænset på den måde 
Opgaven var delt op i mindre issues så vi nemmere kunne følge med i hvad der blev implementeret af agenten.
Dette gjorde vi ved at først give den issues og lade den lave en implementeringsplan ud fra issuet så vi kunne se dens 'tankegange'.
Ved at vi lavede små issues ad gangen havde vi bedre overblik over den skrevne kode og kunne nemmere rette til hvis der var behov for det.

#### Et forslag eller en ændring fra AI som vi accepterede 
Issue - Implementer tilfældige sensordata
Vi fik et ekstra fra review af Copilot, hvor den foreslog at vi skulle tilføje en begrænsing på vores parseMessage metode inde i SensorHandler.
Førhen havde vi at formatet kunne have en uendelig lang besked uden begrænsning, så den kom med det forslag at vi skulle afgrænse så det kun var beskeder med
bestemt format der kunne accepteres.

#### Et forslag eller en ændring som vi ændrede eller afviste 
I SensorLog havde agenten implementeret metoder der indeholdte 'Path' og 'Files' som vi ikke havde så meget erfaring med.
Vi promtede den derfor til at ændre det så det passede med en log vi havde fra en tidligere opgave.

#### Hvordan vi testede at AI-genereret kode virkede 
Vi fik agenten til at lave nogle unit test som vi kunne køre løbende gennem implementering for at tjekke at al koden virkede som det skulle.
Det andet vi gjorde var selv at teste systemet manuelt for at sikre at alle funktioner fungerede som de skulle.

## Testresultater
| Test            | Resultat      |
| :-------------: |:-------------:|
| En sensorklient sender data       | Sender data i format: [O2: 19.2 %] |
| Flere sensorklienter sender data       | Sender data lige efter hinanden uden fejl: [PRESSURE: 936.7 hPa] [TEMP: 26.6 °C]      |
| Værdier uden for grænsen får alarm  | Tydelig fejlbesked sendes hvis uden fro grænse: [ALARM: TEMP: -18.8 °C]      |
| Log af data | Logformat: 2026-09-22 21:13:12 - Received DATA from /127.0.0.1:52042: [PRESSURE: 936.7 hPa]|
| Log af alarm | Logformat: 2026-09-22 21:13:14 - Threshold alarm from /127.0.0.1:52041: ALARM: O2:27.2 % outside allowed range [19.0 - 23.0]|

