# DAM model ter izračun statističnih povzetkov
Ideja naloge je, da iz vhodnih podatkov čimbolj optimalno preberete točke, ki pripadajo določenemu območju (čimmanj klicev za branje datoteke). Nad izbranimi točkami boste potem naredili statistične povzetke. Da boste dosegli čimbolj optimalno branje točk iz vhodnih podatkov boste najprej definirali podatkovno strukturo ter v 1. programu vhodne podatke predobdelali glede na to strukturo.

V drugem programu boste optimizirano brali predobdelane podatke ter izvedli statistične povzetke.

### Struktura podanih podatkov
Vsaka vrstica nosi podatke za točko in sicer položaj točke v 3D svetu (x,y,z) ter intenziteto odboja (i). Vrednosti za posamezno točko so ločene s presledkom. Podatki vsake točke pa so ločeni z novo vrstico.

`x y z i`

##### Primer vhodne datoteke s šestimi točkami:
```
394372.82 39305.84 235.98 34
394372.82 39306.06 234.95 37
394372.81 39308.25 246.70 5
394372.81 39310.52 236.67 40
394372.82 39316.89 238.30 60
394372.81 39319.95 241.38 36
```
Gradnja predpomnilniško zavedne podatkovne strukture ter preobdelava podatkov
V 1. programu spremenite podatkovno strukturo vhodnih podatkov. Podatki naj bodo strukturirani in urejeni tako, da bo branje obdelanih podatkov čimbolj optimalno. Obdelane vhodne podatke zapišite v novo datoteko. Zaželjeno je, da je nova datoteka manjša od originalne (namesto ascii strukture lahko imate binarno), v kolikor pa imate večjo, pa naj ne presega originalne za več kot 5% velikosti!

##### Kot vhod boste prejeli:
```
<vhodna_datoteka>   - datoteka z vhodnimi podatki
<izhodna_datoteka>  - datoteka z izhodnimi obdelanimi podatki
```
##### Primer klica programa:
```
program1 <vhodna_datoteka.txt> <obdelani_podatki>
```

## Optimalno branje podatkov ter izračun statističnih povzetkov podatkov
V 2. programu boste naredili optimalno branje predobdelanih podatkov. Pri zagonu bo uporabnik določil podatke o "velikosti pomnilnika", katerega program ne sme prekoračiti ter še podatek o velikosti bloka, ki nam bo določal velikost bloka podatkov, ki se bo naenkrat prebral iz datoteke (vhodni podatek bo celo število npr. 1 -> kar bo pomenilo 1MB). Uporabnik bo določil tudi območje znotraj katerega boste poiskali točke. Samo iskanje točk naj bo narejeno čimbolj optimalno. Nad točkami izvedite še spodaj opisane statistične povzetke (kateri podatki se bodo uporabili za izračun statistike so določeni z argumentom <opcija>, sam histogram pa naj ima velikost okna določeno z argumentom <velikost_koša>).

##### Kot vhod boste prejeli:
```
<obdelani_podatki>  - datoteka s preobdelanimi podatki
N                   - velikost vhodnih podatkov (to velikost preberete sami in ne bo podana kot argument)
<M>                 - velikost pomnilnika (to velikost določi uporabnik in določa koliko vhodnih podatkov lahko imamo naenkrat v "buffer-ju")
<B>                 - velikost bloka (to velikost določi uporabnik, ter nam določa koliko vhodnih podatkov lahko preberemo naenkrat v aplikacijo)
<minX>              - vrednost X, ki nam bo definiral začetek območja, kjer bomo iskali točke
<maxX>              - vrednost X, ki nam bo definiral konec območja, kjer bomo iskali točke
<minY>              - vrednost Y, ki nam bo definiral začetek območja, kjer bomo iskali točke
<minY>              - vrednost Y, ki nam bo definiral konec območja, kjer bomo iskali točke
<velikost koša>     - velikost koša v histogramu
<opcija>            - možnost izbire uporabnika preko argumenta, ali se za izračun statistike uporabila vrednosti intenzitete (i) ali višine (z)
```
##### Primer klica:
```
program2 <obdelani_podatki> <M> <B> <minX> <maxX> <minY> <maxY> <velikost_koša> <opcija>
```
Izračun statističnih povzetkov bomo izvedli nad atributom intenzitete ali višine (določi se ob klicu programa). Najprej bo potrebno izgraditi histogram, kjer boste kot vhodni parameter pridobili velikost koša (okna) v histogramu. Nato boste zapolnili vrednosti v histogramu z vrednostimi, ki pripadajo točkam znotraj območja.

Točka je znotraj območja, v kolikor velja:
`minX <= x <= minY <= y <= maxY`

Iz histograma morate pa nato pridobiti še naslednje podatke:
- število košev v histogramu,
- povprečno vrednost,
- standardni odklon,
- asimetrijo (Skewness),
- sploščenost (Kurtosis),
- izpišite tudi število točk, ki so znotraj območja!

## Izpis rezultatov
Program 2 naj po zagonu izpiše podatke statistike (število košev v histogramu, povprečje, standardni odklon, asimetrijo, sploščenost...) ter izpisati mora tudi število branj, ki jih je izvedel iz diska.

Aplikaciji sta lahko napisani v jezikih C++, C# ali pa Java.

## Primer izračuna statističnih povzetkov

Primer nad testnimi številkami:
```
testne_številke = {1, 2, 3, 2, 2, 1, 14, 5, 16, 20, 21, 22, 25}
```
##### Izgradnja histograma z velikostjo koša 4
```
K = ( max(testne_številke) - min(testne_številke) / velikost_koša) + 1;
```
Določena vrednost x spada znotraj intervala `[interval_min, interval_max]` v kolikor je `interval_min <= x < interval_max`
```
k       interval koša     H[k]     vre[k]
0       [1 -  5]          6        3
1       [5 -  9]          1        7
2       [9 - 13]          0        11
3       [13 - 17]         2        15
4       [17 - 21]         1        19
5       [21 - 25]         2        23
6       [25 - 29]         1        27
```
##### Izračun povprečja nad podanim histogramom
`povprečje = 11.3077`
##### Izračun standardnega odklona nad podanim histogramom
`standardni odklon = 8.93898`
##### Izračun asimetričnosti nad podanim histogramom
`asimetričnost = 0.391282`
##### Izračun sploščenosti nad podanim histogramom
`sploščenost = -1.61877`
##### Prvi primer zagona statistike nad testno datoteko
```bash
program2 <obdelani_podatki> <M> <B> <minX> <maxX> <minY> <maxY> <velikost_koša> <opcija>
program2 <obdelani_podatki> <M> <B> 394364 394374 39150 39160 5 i
```
###### Predvideni izhodi:
```
Število točk znotraj območja: 1031
Izračunano povprečje: 37.8346
Izračunano standardni odklon: 19.7108
Izračunana asimetričnost: 1.22782
Izračunana sploščenost: 4.83795
```
##### Drugi primer zagona statistike nad testno datoteko
```bash
program2 <obdelani_podatki> <M> <B> 394352 394376 39158 39172 10 i
```
###### Predvideni izhodi:
```
Število točk znotraj območja: 4380
Izračunano povprečje: 38.5183
Izračunano standardni odklon: 16.364
Izračunana asimetričnost: 0.941511
Izračunana sploščenost: 7.30607
```